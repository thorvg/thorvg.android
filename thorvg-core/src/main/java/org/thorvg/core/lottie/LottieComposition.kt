/*
 * Copyright (c) 2025 - 2026 ThorVG project. All rights reserved.

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

package org.thorvg.core.lottie

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import androidx.annotation.FloatRange
import androidx.annotation.RawRes
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * ThorVG-backed Lottie composition.
 *
 * Each subclass owns a native handle bound to a specific renderer.
 * Use [LottieSwComposition] for bitmap output, or [LottieGlComposition] for GL output.
 */
sealed class LottieComposition protected constructor(
    protected val jsonContent: String,
    nativeCreator: (String, IntArray) -> Long
) {
    protected var nativePtr: Long = 0L
        private set

    /**
     * Total frame count reported by the native animation.
     */
    val frameCount: Int

    /**
     * Total animation duration in milliseconds.
     */
    val duration: Long

    /**
     * Last valid frame index that can be rendered.
     */
    val lastFrame: Int
        get() {
            val count = frameCount
            return if (count > 0) count - 1 else 0
        }

    init {
        val outValues = IntArray(LOTTIE_INFO_COUNT)
        nativePtr = nativeCreator(jsonContent, outValues)
        frameCount = outValues[LOTTIE_INFO_FRAME_COUNT]
        duration = outValues[LOTTIE_INFO_DURATION].toLong()
    }

    /**
     * Configures the output dimensions used for subsequent frame renders.
     */
    abstract fun setSize(width: Int, height: Int)

    /**
     * Releases the underlying native animation and any renderer-specific buffers.
     */
    open fun release() {
        val ptr = nativePtr
        if (ptr == 0L) return

        nativePtr = 0L
        LottieNativeBindings.nDestroyLottie(ptr)
    }

    fun isValid(): Boolean = nativePtr != 0L

    companion object {
        private const val LOTTIE_INFO_FRAME_COUNT = 0
        private const val LOTTIE_INFO_DURATION = 1
        private const val LOTTIE_INFO_COUNT = 2

        @Throws(IOException::class)
        internal fun loadJsonFile(resources: Resources, @RawRes resId: Int): String {
            return try {
                resources.openRawResource(resId).use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        reader.lineSequence().joinToString("")
                    }
                }
            } catch (e: IOException) {
                throw IOException("Failed to read a lottie file.")
            }
        }
    }
}

/**
 * Lottie composition that renders into an Android [Bitmap] via ThorVG's SW canvas.
 */
class LottieSwComposition(jsonContent: String) :
    LottieComposition(jsonContent, LottieNativeBindings::nCreateSwLottie) {

    private var buffer: Bitmap? = null

    /**
     * Allocates or resizes the backing buffer used for frame rendering.
     */
    override fun setSize(width: Int, height: Int) {
        if (!isValid()) return
        if (width <= 0 || height <= 0) return

        val existing = buffer
        if (existing != null &&
            !existing.isRecycled &&
            existing.width == width &&
            existing.height == height
        ) {
            return
        }

        existing?.recycle()
        val newBuffer = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        buffer = newBuffer
        LottieNativeBindings.nResizeSwLottie(nativePtr, newBuffer, width.toFloat(), height.toFloat())
    }

    /**
     * Renders the requested frame into the current backing bitmap.
     */
    fun renderFrame(frame: Int): Bitmap? {
        if (!isValid()) return null
        buffer?.let {
            LottieNativeBindings.nDrawSwLottieFrame(nativePtr, it, frame)
        }
        return buffer
    }

    /**
     * Creates a new composition backed by a separate native handle using the same source content.
     */
    fun copy(): LottieSwComposition = LottieSwComposition(jsonContent)

    override fun release() {
        buffer?.recycle()
        buffer = null
        super.release()
    }

    companion object {
        /**
         * Creates a SW composition from a raw resource that contains Lottie JSON.
         */
        @JvmStatic
        fun fromRawResource(resources: Resources, @RawRes resId: Int): LottieSwComposition {
            return LottieSwComposition(loadJsonFile(resources, resId))
        }
    }
}

/**
 * Lottie composition that renders into an externally provided GL framebuffer via ThorVG's GL canvas.
 *
 * The owner is responsible for making the EGL context current and binding the framebuffer
 * before calling [renderFrame].
 */
class LottieGlComposition(jsonContent: String) :
    LottieComposition(jsonContent, LottieNativeBindings::nCreateGlLottie) {

    private var width = 0
    private var height = 0
    private var boundDisplay = 0L
    private var boundSurface = 0L
    private var boundContext = 0L
    private var boundFramebufferId = -1
    private var boundWidth = 0
    private var boundHeight = 0

    /**
     * Records the output dimensions. The native binding is applied by the next [target] call.
     */
    override fun setSize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        this.width = width
        this.height = height
    }

    /**
     * Binds the GL render target (EGL handles + framebuffer id) for subsequent [renderFrame] calls.
     *
     * Caller must have the EGL context current. Uses the dimensions set by [setSize].
     */
    fun target(display: Long, surface: Long, context: Long, framebufferId: Int): Boolean {
        if (!isValid()) return false
        if (width <= 0 || height <= 0) return false

        if (boundDisplay == display &&
            boundSurface == surface &&
            boundContext == context &&
            boundFramebufferId == framebufferId &&
            boundWidth == width &&
            boundHeight == height
        ) {
            return true
        }

        val resized = LottieNativeBindings.nResizeGlLottie(
            nativePtr,
            display,
            surface,
            context,
            framebufferId,
            width.toFloat(),
            height.toFloat()
        )
        if (resized) {
            boundDisplay = display
            boundSurface = surface
            boundContext = context
            boundFramebufferId = framebufferId
            boundWidth = width
            boundHeight = height
        }
        return resized
    }

    /**
     * Renders the requested frame into the bound GL framebuffer.
     */
    fun renderFrame(frame: Int): Boolean {
        if (!isValid()) return false
        LottieNativeBindings.nDrawGlLottieFrame(nativePtr, frame)
        return true
    }

    override fun release() {
        width = 0
        height = 0
        boundDisplay = 0L
        boundSurface = 0L
        boundContext = 0L
        boundFramebufferId = -1
        boundWidth = 0
        boundHeight = 0
        super.release()
    }

    companion object {
        /**
         * Creates a GL composition from a raw resource that contains Lottie JSON.
         */
        @JvmStatic
        fun fromRawResource(resources: Resources, @RawRes resId: Int): LottieGlComposition {
            return LottieGlComposition(loadJsonFile(resources, resId))
        }
    }
}

/**
 * Shared mutable playback state used by UI adapters.
 */
class LottieRenderState {
    var repeatMode = LottieConstants.RESTART
        set(@LottieRepeatMode value) {
            field = value
            framesPerUpdate = if (field == LottieConstants.RESTART) 1 else -1
        }

    var repeatCount = 0

    var speed = 1f
        set(@FloatRange(from = 0.0) value) {
            field = value
            updateFrameInterval()
        }

    var firstFrame = 0
        private set
    var lastFrame = 0
        private set

    var frameInterval = 0L
    var framesPerUpdate = 1
    var autoPlay = false

    var composition: LottieComposition? = null
        set(value) {
            field = value
            updateFrameInterval()
        }

    fun copyPlaybackTo(target: LottieRenderState) {
        target.repeatMode = repeatMode
        target.repeatCount = repeatCount
        target.speed = speed
        target.framesPerUpdate = framesPerUpdate
        target.autoPlay = autoPlay
    }

    fun setFrameBounds(firstFrame: Int, lastFrame: Int? = null) {
        val resolvedFirstFrame = firstFrame.coerceAtLeast(0)
        val compositionLastFrame = composition?.lastFrame
        if (compositionLastFrame == null) {
            this.firstFrame = resolvedFirstFrame
            this.lastFrame = lastFrame?.coerceAtLeast(0) ?: 0
        } else {
            val resolvedLastFrame = (lastFrame ?: compositionLastFrame)
                .coerceAtLeast(resolvedFirstFrame)
                .coerceAtMost(compositionLastFrame)
            this.firstFrame = resolvedFirstFrame.coerceAtMost(resolvedLastFrame)
            this.lastFrame = resolvedLastFrame
        }
        updateFrameInterval()
    }

    private fun updateFrameInterval() {
        val totalFrames = lastFrame - firstFrame + 1
        val compositionDuration = composition?.duration ?: 0L
        frameInterval = when {
            compositionDuration <= 0L -> 0L
            totalFrames <= 0 -> 0L
            speed <= 0f -> 0L
            else -> (compositionDuration / totalFrames / speed).toLong()
        }
    }

}
