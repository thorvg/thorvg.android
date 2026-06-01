package org.thorvg.core.lottie

import android.graphics.Bitmap

object LottieNativeBindings {
    init {
        System.loadLibrary("lottie-libs")
    }

    @JvmStatic
    external fun nCreateLottie(
        content: String?,
        outValues: IntArray?
    ): Long

    @JvmStatic
    external fun nSetLottieBufferSize(
        lottiePtr: Long,
        bitmap: Bitmap?,
        width: Float,
        height: Float
    )

    @JvmStatic
    external fun nDrawLottieFrame(lottiePtr: Long, bitmap: Bitmap, frame: Int)

    @JvmStatic
    external fun nDestroyLottie(lottiePtr: Long)
}