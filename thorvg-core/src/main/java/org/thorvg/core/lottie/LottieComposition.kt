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
import androidx.annotation.RestrictTo
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
    protected val nativePtr: Long

    /**
     * Total frame count reported by the native animation.
     */
    val frameCount: Int

    /**
     * Total animation duration in milliseconds.
     */
    val duration: Long

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
        if (nativePtr != 0L) {
            LottieNativeBindings.nDestroyLottie(nativePtr)
        }
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
        require(width > 0) { "LottieSwComposition requires width > 0" }
        require(height > 0) { "LottieSwComposition requires height > 0" }
        buffer?.recycle()
        buffer = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        LottieNativeBindings.nResizeSwLottie(nativePtr, buffer, width.toFloat(), height.toFloat())
    }

    /**
     * Renders the requested frame into the current backing bitmap.
     */
    fun renderFrame(frame: Int): Bitmap? {
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
    private var boundTarget: LottieRenderTarget.Gl? = null
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
    fun target(target: LottieRenderTarget.Gl): Boolean {
        if (!isValid()) return false
        if (width <= 0 || height <= 0) return false

        if (boundTarget == target && boundWidth == width && boundHeight == height) {
            return true
        }

        val resized = LottieNativeBindings.nResizeGlLottie(
            nativePtr,
            target.display,
            target.surface,
            target.context,
            target.framebufferId,
            width.toFloat(),
            height.toFloat()
        )
        if (resized) {
            boundTarget = target
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
        boundTarget = null
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class LottieRenderTarget {
    data class Gl(
        val display: Long,
        val surface: Long,
        val context: Long,
        val framebufferId: Int
    ) : LottieRenderTarget()
}

/**
 * Shared mutable playback state used by UI adapters.
 *
 * Concrete subclasses pair the state with a specific composition type:
 * [LottieSwRenderState] for bitmap output, [LottieGlRenderState] for GL output.
 */
sealed class LottieRenderState<C : LottieComposition> {
    var composition: C? = null
        set(value) {
            if (field === value) return
            field = value
            updateFrameInterval()
        }

    var baseWidth = 0f
    var baseHeight = 0f

    var width = 0
    var height = 0

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
        set(value) {
            field = value
            updateFrameInterval()
        }

    var lastFrame = 0
        set(value) {
            field = value
            updateFrameInterval()
        }

    var frameInterval = 0L
    var framesPerUpdate = 1
    var autoPlay = false

    fun release() {
        composition?.release()
        composition = null
    }

    fun valid(): Boolean = composition?.isValid() == true

    fun setSize(width: Int, height: Int) {
        if (width != this.width || height != this.height) {
            this.width = width
            this.height = height
            composition?.setSize(width, height)
        }
    }

    fun resolvedLastFrame(): Int {
        return resolveLastFrame(composition?.frameCount ?: 0, firstFrame, lastFrame)
    }

    fun copyPlaybackTo(target: LottieRenderState<*>) {
        target.baseWidth = baseWidth
        target.baseHeight = baseHeight
        target.width = width
        target.height = height
        target.repeatMode = repeatMode
        target.repeatCount = repeatCount
        target.speed = speed
        target.firstFrame = firstFrame
        target.lastFrame = lastFrame
        target.framesPerUpdate = framesPerUpdate
        target.autoPlay = autoPlay
    }

    protected fun updateFrameInterval() {
        val currentComposition = composition
        val totalFrames = inclusiveFrameCount(firstFrame, resolvedLastFrame())
        frameInterval = when {
            currentComposition == null -> 0L
            totalFrames <= 0 -> 0L
            speed <= 0f -> 0L
            else -> (currentComposition.duration / totalFrames / speed).toLong()
        }
    }

    internal companion object {
        fun resolveLastFrame(frameCount: Int, firstFrame: Int, lastFrame: Int): Int {
            if (frameCount <= 0) return firstFrame
            val maxFrame = frameCount - 1
            if (maxFrame < firstFrame) return firstFrame
            return (if (lastFrame > 0) lastFrame else maxFrame)
                .coerceAtLeast(firstFrame)
                .coerceAtMost(maxFrame)
        }

        fun inclusiveFrameCount(firstFrame: Int, lastFrame: Int): Int {
            return lastFrame - firstFrame + 1
        }
    }
}

/**
 * Playback state paired with a [LottieSwComposition]; renders into a bitmap buffer.
 */
class LottieSwRenderState : LottieRenderState<LottieSwComposition>() {
    /**
     * Renders the requested frame into the composition's backing bitmap.
     */
    fun renderFrame(frame: Int): Bitmap? = composition?.renderFrame(frame)

    companion object {
        /**
         * Creates a SW render state preloaded with a composition from a raw resource.
         */
        @JvmStatic
        fun fromRawResource(resources: Resources, @RawRes resId: Int): LottieSwRenderState {
            return LottieSwRenderState().apply {
                composition = LottieSwComposition.fromRawResource(resources, resId)
            }
        }
    }
}

/**
 * Playback state paired with a [LottieGlComposition]; renders into a bound GL framebuffer.
 */
class LottieGlRenderState : LottieRenderState<LottieGlComposition>() {
    /**
     * Binds the GL render target and applies the current size to the native renderer.
     *
     * Caller must have the EGL context current.
     */
    fun target(target: LottieRenderTarget.Gl): Boolean =
        composition?.target(target) == true

    /**
     * Renders the requested frame into the bound GL framebuffer.
     */
    fun renderFrame(frame: Int): Boolean = composition?.renderFrame(frame) == true

    companion object {
        /**
         * Creates a GL render state preloaded with a composition from a raw resource.
         */
        @JvmStatic
        fun fromRawResource(resources: Resources, @RawRes resId: Int): LottieGlRenderState {
            return LottieGlRenderState().apply {
                composition = LottieGlComposition.fromRawResource(resources, resId)
            }
        }
    }
}
