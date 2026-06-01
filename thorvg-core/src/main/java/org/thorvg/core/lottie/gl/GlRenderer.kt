/*
 * Copyright (c) 2026 ThorVG project. All rights reserved.

 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.thorvg.core.lottie.gl

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.os.Handler
import android.os.Looper
import androidx.annotation.RestrictTo
import org.thorvg.core.lottie.LottieConstants
import org.thorvg.core.lottie.LottieGlComposition
import org.thorvg.core.lottie.LottieGlRenderState
import org.thorvg.core.lottie.LottieRenderTarget
import java.util.concurrent.CountDownLatch
import kotlin.math.floor

/**
 * Drives a Lottie GL render loop on the shared GL thread.
 *
 * The hosting view/composable wires the render surface, composition factory, and
 * playback callbacks; this class handles EGL surface binding, frame pacing,
 * repeat/end signaling, and target rebinding.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GlRenderer(
    private val onAnimationStart: () -> Unit = {},
    private val onAnimationEnd: () -> Unit = {},
    private val onAnimationRepeat: () -> Unit = {},
    private val onRenderFailure: () -> Unit = {}
) : SharedGlThread.RenderClient {
    private val sharedGl = SharedGlThread.instance
    private val handler = sharedGl.handler
    private val mainHandler = Handler(Looper.getMainLooper())

    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var compositionFactory: (() -> LottieGlComposition?)? = null
    private val renderState = LottieGlRenderState()
    private var width = 0
    private var height = 0
    private var repeated = 0
    private var startDispatched = false
    private var failed = false
    private var surfaceReady = false
    private var dirtyFrame = false
    private var targetDirty = true

    // Time anchor: currentFrame is derived from the vsync timestamp relative to beginTimeNanos/beginFrame.
    private var beginTimeNanos = 0L
    private var beginFrame = 0
    private var beginTimePending = false
    private var renderedFrame = -1

    @Volatile
    private var playbackState = PlaybackState.Idle

    val isRunning: Boolean
        get() = playbackState == PlaybackState.Running

    @Volatile
    var currentFrame = 0
        private set

    fun setSurface(surface: SurfaceTexture, width: Int, height: Int) {
        post {
            val shouldStart = isRunning || renderState.autoPlay
            clearSurfaceOnGlThread(releaseComposition = false)
            failed = false

            eglSurface = sharedGl.createWindowSurface(surface)
            if (eglSurface == EGL14.EGL_NO_SURFACE) {
                notifyRenderFailure()
                return@post
            }

            this.width = width
            this.height = height
            surfaceReady = true
            targetDirty = true

            if (!ensureComposition()) {
                notifyRenderFailure()
                return@post
            }
            if (!bindTarget()) {
                notifyRenderFailure()
                return@post
            }

            sharedGl.register(this)
            if (shouldStart) {
                startRunning(resetPosition = true)
            } else {
                dirtyFrame = true
                sharedGl.requestRender()
            }
        }
    }

    fun resize(width: Int, height: Int) {
        post {
            if (!surfaceReady) return@post
            if (this.width == width && this.height == height) return@post

            this.width = width
            this.height = height
            targetDirty = true
            if (!bindTarget()) {
                notifyRenderFailure()
                return@post
            }
            dirtyFrame = true
            sharedGl.requestRender()
        }
    }

    fun clearSurface() {
        postAndWait {
            clearSurfaceOnGlThread(releaseComposition = true)
        }
    }

    fun setCompositionFactory(
        compositionFactory: (() -> LottieGlComposition?)?,
        state: LottieGlRenderState
    ) {
        post {
            val shouldStart = isRunning || state.autoPlay
            failed = false
            renderState.release()
            this.compositionFactory = compositionFactory
            state.copyPlaybackTo(renderState)
            repeated = 0
            startDispatched = false
            dirtyFrame = false
            targetDirty = true
            currentFrame = renderState.firstFrame
            resetBeginTime()

            if (!ensureComposition()) {
                notifyRenderFailure()
                return@post
            }
            if (!bindTarget()) {
                notifyRenderFailure()
                return@post
            }
            if (renderState.composition != null && shouldStart) {
                startRunning(resetPosition = false)
            } else {
                playbackState = PlaybackState.Idle
                dirtyFrame = true
                sharedGl.requestRender()
            }
        }
    }

    fun setConfig(state: LottieGlRenderState) {
        post {
            state.copyPlaybackTo(renderState)
            currentFrame = currentFrame.coerceIn(renderState.firstFrame, lastFrame())
            resetBeginTime()
            dirtyFrame = true
            sharedGl.requestRender()
        }
    }

    fun start() {
        post { startRunning(resetPosition = true) }
    }

    fun stop() {
        post {
            playbackState = PlaybackState.Idle
            startDispatched = false
            repeated = 0
            currentFrame = renderState.firstFrame
            dirtyFrame = true
            sharedGl.requestRender()
        }
    }

    fun pause() {
        post {
            if (playbackState == PlaybackState.Running) {
                playbackState = PlaybackState.Paused
            }
        }
    }

    fun resume() {
        post {
            if (playbackState != PlaybackState.Running) {
                playbackState = PlaybackState.Running
                resetBeginTime()
                dirtyFrame = true
                sharedGl.requestRender()
            }
        }
    }

    fun release() {
        postAndWait {
            playbackState = PlaybackState.Idle
            clearSurfaceOnGlThread(releaseComposition = true)
        }
        mainHandler.removeCallbacksAndMessages(null)
    }

    override fun shouldRender(): Boolean =
        surfaceReady && !failed && renderState.composition != null && (dirtyFrame || isRunning)

    private fun startRunning(resetPosition: Boolean) {
        if (resetPosition) {
            currentFrame = renderState.firstFrame
            repeated = 0
        }
        playbackState = PlaybackState.Running
        startDispatched = false
        resetBeginTime()
        dirtyFrame = true
        sharedGl.requestRender()
    }

    /** Anchors the begin time to the current frame; the vsync timestamp is captured on the next [onRenderFrame]. */
    private fun resetBeginTime() {
        beginFrame = currentFrame
        beginTimePending = true
    }

    private fun ensureComposition(): Boolean {
        if (renderState.composition != null || compositionFactory == null) return true
        if (!surfaceReady) return true
        if (!sharedGl.makeCurrent(eglSurface)) return false

        val composition = compositionFactory?.invoke()
        renderState.composition = composition
        return composition?.isValid() == true
    }

    private fun bindTarget(ensureCurrent: Boolean = true): Boolean {
        if (renderState.composition == null) return true
        if (!surfaceReady || width <= 0 || height <= 0) return true
        if (ensureCurrent && !sharedGl.makeCurrent(eglSurface)) return false

        val target = LottieRenderTarget.Gl(
            display = sharedGl.eglDisplay.nativeHandle,
            surface = eglSurface.nativeHandle,
            context = sharedGl.eglContext.nativeHandle,
            framebufferId = DEFAULT_FRAMEBUFFER
        )
        renderState.setSize(width, height)
        if (!renderState.target(target)) return false
        targetDirty = false
        return true
    }

    override fun onRenderFrame(frameTimeNanos: Long): Boolean {
        if (!surfaceReady || width <= 0 || height <= 0) return false
        if (renderState.composition == null) return false

        val running = playbackState == PlaybackState.Running
        if (!running && !dirtyFrame) return false

        // Derive the frame from elapsed vsync time, so playback follows wall-clock
        // time and skips ahead correctly when render frames are dropped.
        if (running) {
            currentFrame = computeFrame(frameTimeNanos)
        }

        // Skip redundant GPU work when the visible frame has not changed.
        if (running && !dirtyFrame && startDispatched && currentFrame == renderedFrame) {
            return false
        }

        // Make the context current and (re)bind the target only when needed.
        val needsTargetBind = targetDirty || sharedGl.lastRenderedClient != this
        if (!sharedGl.makeCurrent(eglSurface) ||
            (needsTargetBind && !bindTarget(ensureCurrent = false))
        ) {
            notifyRenderFailure()
            return false
        }

        // Render the current frame straight into the window surface.
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, DEFAULT_FRAMEBUFFER)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (!renderState.renderFrame(currentFrame) || !sharedGl.swapBuffers(eglSurface)) {
            notifyRenderFailure()
            return false
        }
        renderedFrame = currentFrame
        dirtyFrame = false

        if (running && !startDispatched) {
            startDispatched = true
            mainHandler.post { onAnimationStart() }
        }
        return true
    }

    /**
     * Computes the integer frame to display at [frameTimeNanos], measured from the
     * begin time. Handles the playback segment, direction, looping and end.
     *
     *     frame = beginFrame ± elapsedSeconds * fps * speed
     *
     * where fps is derived from the full animation (frameCount / duration), so a
     * segment plays at the same per-frame rate as the whole animation.
     */
    private fun computeFrame(frameTimeNanos: Long): Int {
        val composition = renderState.composition ?: return currentFrame
        val first = renderState.firstFrame
        val last = lastFrame()
        val durationMs = composition.duration
        if (last <= first || durationMs <= 0L || composition.frameCount <= 1) {
            return first
        }

        // Capture the vsync timestamp lazily on the first frame of a sweep.
        if (beginTimePending) {
            beginTimeNanos = frameTimeNanos
            beginTimePending = false
        }

        val elapsedSec = (frameTimeNanos - beginTimeNanos).coerceAtLeast(0L) / NANOS_PER_SECOND
        val fps = composition.frameCount.toDouble() / (durationMs / MILLIS_PER_SECOND)
        val deltaFrames = elapsedSec * fps * renderState.speed

        val forward = renderState.framesPerUpdate >= 0
        val raw = beginFrame + if (forward) deltaFrames else -deltaFrames
        // Loop/end boundary is exclusive (last + 1 / first - 1) so the segment plays
        // for its full duration: a [0..last] segment spans last + 1 frames of time,
        // matching totalFrame in the native engine. The rendered frame is still
        // clamped to [first, last].
        val reachedEnd = if (forward) raw >= last + 1 else raw <= first - 1
        if (reachedEnd) {
            val canLoop = renderState.repeatCount == LottieConstants.INFINITE ||
                repeated < renderState.repeatCount
            if (canLoop) {
                // Loop: re-anchor the begin time at the start of the next sweep.
                repeated++
                beginFrame = if (forward) first else last
                beginTimeNanos = frameTimeNanos
                mainHandler.post { onAnimationRepeat() }
                return beginFrame
            }
            playbackState = PlaybackState.Ended
            mainHandler.post { onAnimationEnd() }
            return if (forward) last else first
        }

        // floor: frame N holds for its full time window [N, N+1), giving an even
        // per-frame duration (round would shift frame boundaries by half a frame).
        return floor(raw).toInt().coerceIn(first, last)
    }

    private fun lastFrame(): Int {
        return renderState.resolvedLastFrame()
    }

    private fun clearSurfaceOnGlThread(releaseComposition: Boolean) {
        sharedGl.unregisterOnGlThread(this)
        surfaceReady = false
        playbackState = PlaybackState.Idle
        dirtyFrame = false
        targetDirty = true

        // Ensure glDelete* has a current context.
        if (!sharedGl.makeCurrent(eglSurface)) {
            sharedGl.makeDefaultCurrent()
        }
        if (releaseComposition) {
            renderState.release()
        }
        if (eglSurface != EGL14.EGL_NO_SURFACE) {
            sharedGl.makeDefaultCurrent()
            sharedGl.destroyWindowSurface(eglSurface)
            eglSurface = EGL14.EGL_NO_SURFACE
        }
    }

    private fun notifyRenderFailure() {
        if (failed) return

        failed = true
        playbackState = PlaybackState.Idle
        dirtyFrame = false
        sharedGl.unregisterOnGlThread(this)
        mainHandler.post { onRenderFailure() }
    }

    private fun post(action: () -> Unit) {
        handler.post(action)
    }

    private fun postAndWait(action: () -> Unit) {
        if (Looper.myLooper() == handler.looper) {
            action()
            return
        }

        val latch = CountDownLatch(1)
        handler.post {
            try {
                action()
            } finally {
                latch.countDown()
            }
        }
        latch.await()
    }

    /**
     * Playback lifecycle of the renderer.
     *
     * - [Idle]: stopped (or never started); positioned at the first frame.
     * - [Running]: actively advancing frames.
     * - [Paused]: halted while keeping the current frame.
     * - [Ended]: finished all repeats; positioned at the last frame.
     */
    private enum class PlaybackState {
        Idle,
        Running,
        Paused,
        Ended
    }

    private companion object {
        // Render straight into the window surface's default framebuffer.
        const val DEFAULT_FRAMEBUFFER = 0

        const val NANOS_PER_SECOND = 1_000_000_000.0
        const val MILLIS_PER_SECOND = 1000.0
    }
}
