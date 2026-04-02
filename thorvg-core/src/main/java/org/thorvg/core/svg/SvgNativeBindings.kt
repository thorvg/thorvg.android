package org.thorvg.core.svg

import android.graphics.Bitmap

object SvgNativeBindings {
    init {
        System.loadLibrary("lottie-libs")
    }

    @JvmStatic
    external fun nCreateSvg(
        content: String?,
        length: Int,
        outValues: IntArray?
    ): Long

    @JvmStatic
    external fun nSetSvgBufferSize(
        svgPtr: Long,
        bitmap: Bitmap?,
        width: Float,
        height: Float
    )

    @JvmStatic
    external fun nDrawSvg(svgPtr: Long, bitmap: Bitmap)

    @JvmStatic
    external fun nDestroySvg(svgPtr: Long)
}
