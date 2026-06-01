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
import org.thorvg.core.lottie.LottieConstants
import org.thorvg.core.lottie.LottieGlComposition
import org.thorvg.core.lottie.LottieRenderState
import org.thorvg.core.lottie.gl.internal.SharedGlThread
import java.util.concurrent.CountDownLatch
import kotlin.math.floor

/**
 * Drives a Lottie GL render loop on the shared GL thread.
 *
 * The hosting view/composable wires the render surface, composition factory, and
 * playback callbacks; this class handles EGL surface binding, frame pacing,
 * repeat/end signaling, and target rebinding.
 */
class GlRenderer(
    private val onAnimationStart: () -> Unit = {},
    private val onAnimationEnd: () -> Unit = {},
    private val onAnimationRepeat: () -> Unit = {},
    private val onRenderFailure: () -> Unit = {}
) {
    private val sharedGl = SharedGlThread.instance
    private val handler = sharedGl.handler
    private val mainHandler = Handler(Looper.getMainLooper())
    private val renderClient = object : SharedGlThread.RenderClient {
        override fun shouldRender(): Boolean = this@GlRenderer.shouldRender()
        override fun onRenderFrame(frameTimeNanos: Long): Boolean =
            this@GlRenderer.onRenderFrame(frameTimeNanos)
    }

    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var compositionRequest: CompositionRequest? = null
    private var composition: LottieGlComposition? = null
    private val renderState = LottieRenderState()
    private var width = 0
    private var height = 0
    private var repeated = 0
    private var startDispatched = false
    private var failed = false
    private var surfaceReady = false
    private var playRequested = false
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

            renderWhenReady(resetPosition = true)
        }
    }

    fun resize(width: Int, height: Int) {
        post {
            if (!surfaceReady || (this.width == width && this.height == height)) return@post

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
        state: LottieRenderState,
        firstFrame: Int,
        lastFrame: Int?
    ) {
        post {
            failed = false
            releaseComposition()
            compositionRequest = compositionFactory?.let {
                CompositionRequest(it, firstFrame, lastFrame)
            }
            state.copyPlaybackTo(renderState)
            repeated = 0
            startDispatched = false
            dirtyFrame = false
            targetDirty = true
            currentFrame = firstFrame.coerceAtLeast(0)
            resetBeginTime()

            renderWhenReady(resetPosition = false)
        }
    }

    fun setConfig(state: LottieRenderState, firstFrame: Int, lastFrame: Int?) {
        post {
            state.copyPlaybackTo(renderState)
            compositionRequest = compositionRequest?.copy(firstFrame = firstFrame, lastFrame = lastFrame)
            renderState.setFrameBounds(firstFrame, lastFrame)
            currentFrame = if (composition != null) {
                currentFrame.coerceIn(renderState.firstFrame, renderState.lastFrame)
            } else {
                renderState.firstFrame
            }
            resetBeginTime()
            dirtyFrame = true
            sharedGl.requestRender()
        }
    }

    fun start() {
        post {
            playRequested = true
            renderWhenReady(resetPosition = true)
        }
    }

    fun stop() {
        post {
            playRequested = false
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
            playRequested = false
            if (playbackState == PlaybackState.Running) {
                playbackState = PlaybackState.Paused
            }
        }
    }

    fun resume() {
        post {
            playRequested = true
            renderWhenReady(resetPosition = false)
        }
    }

    fun release() {
        postAndWait {
            playRequested = false
            playbackState = PlaybackState.Idle
            clearSurfaceOnGlThread(releaseComposition = true)
        }
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun shouldRender(): Boolean =
        surfaceReady && !failed && composition != null && (dirtyFrame || isRunning)

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
        if (composition != null) return true
        val request = compositionRequest ?: return false
        if (!surfaceReady) return false
        if (!sharedGl.makeCurrent(eglSurface)) return false

        val nextComposition = request.factory()
        if (nextComposition?.isValid() != true) return false

        composition = nextComposition
        renderState.composition = nextComposition
        renderState.setFrameBounds(request.firstFrame, request.lastFrame)
        currentFrame = renderState.firstFrame
        return true
    }

    private fun renderWhenReady(resetPosition: Boolean) {
        if (!surfaceReady || (compositionRequest == null && composition == null)) {
            playbackState = PlaybackState.Idle
            return
        }

        if (!ensureComposition()) {
            notifyRenderFailure()
            return
        }
        if (!bindTarget()) {
            notifyRenderFailure()
            return
        }

        sharedGl.register(renderClient)
        if (playRequested || renderState.autoPlay) {
            startRunning(resetPosition)
        } else {
            playbackState = PlaybackState.Idle
            dirtyFrame = true
            sharedGl.requestRender()
        }
    }

    private fun bindTarget(ensureCurrent: Boolean = true): Boolean {
        val composition = composition ?: return true
        if (!surfaceReady || width <= 0 || height <= 0) return true
        if (ensureCurrent && !sharedGl.makeCurrent(eglSurface)) return false

        composition.setSize(width, height)
        if (!composition.target(
            display = sharedGl.eglDisplay.nativeHandle,
            surface = eglSurface.nativeHandle,
            context = sharedGl.eglContext.nativeHandle,
            framebufferId = DEFAULT_FRAMEBUFFER
        )) return false
        targetDirty = false
        return true
    }

    private fun onRenderFrame(frameTimeNanos: Long): Boolean {
        if (!surfaceReady || width <= 0 || height <= 0 || composition == null) return false

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

        if (!draw()) {
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

    /** Render the current frame straight into the window surface. */
    private fun draw(): Boolean {
        // Make the context current and (re)bind the target only when needed.
        val needsTargetBind = targetDirty || sharedGl.lastRenderedClient != renderClient
        if (!sharedGl.makeCurrent(eglSurface) ||
            (needsTargetBind && !bindTarget(ensureCurrent = false))
        ) {
            return false
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, DEFAULT_FRAMEBUFFER)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        val composition = composition ?: return false
        return composition.renderFrame(currentFrame) && sharedGl.swapBuffers(eglSurface)
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
        val composition = composition ?: return currentFrame
        val first = renderState.firstFrame
        val last = renderState.lastFrame
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
        // Boundary is exclusive (last + 1 / first - 1) so the segment plays its full duration.
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
    private fun clearSurfaceOnGlThread(releaseComposition: Boolean) {
        sharedGl.unregisterOnGlThread(renderClient)
        surfaceReady = false
        playbackState = PlaybackState.Idle
        dirtyFrame = false
        targetDirty = true

        // Ensure glDelete* has a current context.
        if (!sharedGl.makeCurrent(eglSurface)) {
            sharedGl.makeDefaultCurrent()
        }
        if (releaseComposition) {
            releaseComposition()
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
        sharedGl.unregisterOnGlThread(renderClient)
        mainHandler.post { onRenderFailure() }
    }

    private fun releaseComposition() {
        composition?.release()
        composition = null
        renderState.composition = null
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

    private data class CompositionRequest(
        val factory: () -> LottieGlComposition?,
        val firstFrame: Int,
        val lastFrame: Int?
    )

    private companion object {
        // Render straight into the window surface's default framebuffer.
        const val DEFAULT_FRAMEBUFFER = 0

        const val NANOS_PER_SECOND = 1_000_000_000.0
        const val MILLIS_PER_SECOND = 1000.0
    }
}
