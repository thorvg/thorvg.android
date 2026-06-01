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
 * ThorVG-backed Lottie composition that can render frames into a bitmap buffer.
 */
class LottieComposition private constructor(
    private val nativeLottie: NativeLottie
) {
    /**
     * Creates a composition from raw Lottie JSON content.
     */
    constructor(content: String) : this(NativeLottie(content))

    /**
     * Total frame count reported by the native animation.
     */
    val frameCount: Int
        get() = nativeLottie.frameCount

    /**
     * Last valid frame index that can be rendered.
     */
    val lastFrame: Int
        get() = (frameCount - 1).coerceAtLeast(0)

    /**
     * Total animation duration in milliseconds.
     */
    val duration: Long
        get() = nativeLottie.duration

    /**
     * Allocates or resizes the backing buffer used for frame rendering.
     */
    fun setSize(width: Int, height: Int) {
        require(width > 0) { "LottieComposition requires width > 0" }
        require(height > 0) { "LottieComposition requires height > 0" }
        nativeLottie.setBufferSize(width, height)
    }

    /**
     * Renders the requested frame into the current backing bitmap.
     */
    fun renderFrame(frame: Int): Bitmap? {
        return nativeLottie.getBuffer(frame)
    }

    /**
     * Releases the native animation and any allocated bitmap buffer.
     */
    fun release() {
        nativeLottie.destroy()
    }

    /**
     * Creates a new composition instance backed by a separate native handle using the same source content.
     */
    fun copy(): LottieComposition {
        return LottieComposition(nativeLottie.copy())
    }

    fun isValid(): Boolean {
        return nativeLottie.nativePtr != 0L
    }

    companion object {
        /**
         * Creates a composition from a raw resource that contains Lottie JSON.
         */
        @JvmStatic
        fun fromRawResource(resources: Resources, @RawRes resId: Int): LottieComposition {
            return LottieComposition(loadJsonFile(resources, resId))
        }

        @Throws(IOException::class)
        private fun loadJsonFile(resources: Resources, @RawRes resId: Int): String {
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
 * Shared mutable playback state used by UI adapters.
 */
class LottieRenderState {
    var composition: LottieComposition? = null

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
            field = value.coerceAtMost(lastFrame)
            updateFrameInterval()
        }

    var lastFrame = 0
        set(value) {
            composition?.let {
                field = value.coerceAtLeast(0).coerceAtMost(it.lastFrame)
                updateFrameInterval()
            }
        }

    var frameInterval = 0L
    var framesPerUpdate = 1
    var autoPlay = false

    fun releaseComposition() {
        composition?.release()
        composition = null
    }

    fun valid(): Boolean {
        return composition?.isValid() == true
    }

    fun setCompositionSize(width: Int, height: Int) {
        if (width != this.width || height != this.height) {
            this.width = width
            this.height = height
            composition?.setSize(width, height)
        }
    }

    fun renderFrame(frame: Int): Bitmap? {
        return composition?.renderFrame(frame)
    }

    protected fun updateFrameInterval() {
        val currentComposition = composition ?: return
        val totalFrames = lastFrame - firstFrame + 1
        frameInterval = when {
            totalFrames <= 0 -> 0L
            speed <= 0f -> 0L
            else -> (currentComposition.duration / totalFrames / speed).toLong()
        }
    }
}

private class NativeLottie {
    val jsonContent: String
    val nativePtr: Long
    var frameCount: Int = 0
    var duration: Long = 0
    private var buffer: Bitmap? = null

    constructor(copy: NativeLottie) {
        jsonContent = copy.jsonContent
        nativePtr = init(copy.jsonContent)
        frameCount = copy.frameCount
        duration = copy.duration
    }

    constructor(content: String) {
        jsonContent = content
        nativePtr = init(jsonContent)
    }

    fun copy(): NativeLottie {
        return NativeLottie(this)
    }

    fun destroy() {
        buffer?.recycle()
        buffer = null
        LottieNativeBindings.nDestroyLottie(nativePtr)
    }

    fun setBufferSize(width: Int, height: Int) {
        buffer?.recycle()
        buffer = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        LottieNativeBindings.nSetLottieBufferSize(nativePtr, buffer, width.toFloat(), height.toFloat())
    }

    fun getBuffer(frame: Int): Bitmap? {
        buffer?.let {
            LottieNativeBindings.nDrawLottieFrame(nativePtr, it, frame)
        }
        return buffer
    }

    private fun init(content: String): Long {
        val outValues = IntArray(LOTTIE_INFO_COUNT)
        val pointer = LottieNativeBindings.nCreateLottie(content, content.length, outValues)
        frameCount = outValues[LOTTIE_INFO_FRAME_COUNT]
        duration = outValues[LOTTIE_INFO_DURATION].toLong()
        return pointer
    }

    companion object {
        private const val LOTTIE_INFO_FRAME_COUNT = 0
        private const val LOTTIE_INFO_DURATION = 1
        private const val LOTTIE_INFO_COUNT = 2
    }
}
