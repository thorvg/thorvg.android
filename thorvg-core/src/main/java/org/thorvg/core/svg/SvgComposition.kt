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

package org.thorvg.core.svg

import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import androidx.annotation.RawRes
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * ThorVG-backed SVG composition that can render into a bitmap buffer.
 */
class SvgComposition private constructor(
    private val nativeSvg: NativeSvg
) {
    /**
     * Creates a composition from raw SVG XML content.
     */
    constructor(content: String) : this(NativeSvg(content))

    /**
     * Intrinsic width reported by the SVG picture.
     */
    val width: Int
        get() = nativeSvg.width

    /**
     * Intrinsic height reported by the SVG picture.
     */
    val height: Int
        get() = nativeSvg.height

    /**
     * Allocates or resizes the backing buffer used for rendering.
     */
    fun setSize(width: Int, height: Int) {
        require(width > 0) { "SvgComposition requires width > 0" }
        require(height > 0) { "SvgComposition requires height > 0" }
        nativeSvg.setBufferSize(width, height)
    }

    /**
     * Renders the picture into the current backing bitmap.
     */
    fun render(): Bitmap? {
        return nativeSvg.getBuffer()
    }

    /**
     * Releases the native picture and any allocated bitmap buffer.
     */
    fun release() {
        nativeSvg.destroy()
    }

    /**
     * Creates a new composition instance backed by a separate native handle using the same source content.
     */
    fun copy(): SvgComposition {
        return SvgComposition(nativeSvg.copy())
    }

    fun isValid(): Boolean {
        return nativeSvg.nativePtr != 0L
    }

    companion object {
        /**
         * Creates a composition from a raw resource that contains SVG XML.
         */
        @JvmStatic
        fun fromRawResource(resources: Resources, @RawRes resId: Int): SvgComposition {
            return SvgComposition(loadSvgFile { loadRawResource(resources, resId) })
        }

        /**
         * Creates a composition from an asset file that contains SVG XML.
         */
        @JvmStatic
        fun fromAsset(assetManager: AssetManager, assetName: String): SvgComposition {
            return SvgComposition(loadSvgFile { loadAsset(assetManager, assetName) })
        }

        @Throws(IOException::class)
        private fun loadSvgFile(openInputStream: () -> java.io.InputStream): String {
            return try {
                openInputStream().use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                        reader.readText()
                    }
                }
            } catch (e: IOException) {
                throw IOException("Failed to read an svg file.")
            }
        }

        private fun loadRawResource(resources: Resources, @RawRes resId: Int) =
            resources.openRawResource(resId)

        private fun loadAsset(assetManager: AssetManager, assetName: String) =
            assetManager.open(assetName)
    }
}

private class NativeSvg {
    val xmlContent: String
    val nativePtr: Long
    var width: Int = 0
    var height: Int = 0
    private var buffer: Bitmap? = null

    constructor(copy: NativeSvg) {
        xmlContent = copy.xmlContent
        nativePtr = init(copy.xmlContent)
        width = copy.width
        height = copy.height
    }

    constructor(content: String) {
        xmlContent = content
        nativePtr = init(xmlContent)
    }

    fun copy(): NativeSvg {
        return NativeSvg(this)
    }

    fun destroy() {
        buffer?.recycle()
        buffer = null
        SvgNativeBindings.nDestroySvg(nativePtr)
    }

    fun setBufferSize(width: Int, height: Int) {
        buffer?.recycle()
        buffer = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        SvgNativeBindings.nSetSvgBufferSize(nativePtr, buffer, width.toFloat(), height.toFloat())
    }

    fun getBuffer(): Bitmap? {
        buffer?.let {
            SvgNativeBindings.nDrawSvg(nativePtr, it)
        }
        return buffer
    }

    private fun init(content: String): Long {
        val outValues = IntArray(SVG_INFO_COUNT)
        val pointer = SvgNativeBindings.nCreateSvg(content, content.length, outValues)
        width = outValues[SVG_INFO_WIDTH]
        height = outValues[SVG_INFO_HEIGHT]
        return pointer
    }

    companion object {
        private const val SVG_INFO_WIDTH = 0
        private const val SVG_INFO_HEIGHT = 1
        private const val SVG_INFO_COUNT = 2
    }
}
