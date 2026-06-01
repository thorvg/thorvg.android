package org.thorvg.core.lottie

import android.graphics.Bitmap

object LottieNativeBindings {
    init {
        System.loadLibrary("lottie-libs")
    }

    @JvmStatic
    external fun nCreateSwLottie(
        content: String?,
        outValues: IntArray?
    ): Long

    @JvmStatic
    external fun nCreateGlLottie(
        content: String?,
        outValues: IntArray?
    ): Long

    @JvmStatic
    external fun nResizeSwLottie(
        lottiePtr: Long,
        bitmap: Bitmap?,
        width: Float,
        height: Float
    )

    @JvmStatic
    external fun nResizeGlLottie(
        lottiePtr: Long,
        display: Long,
        surface: Long,
        context: Long,
        framebufferId: Int,
        width: Float,
        height: Float
    ): Boolean

    @JvmStatic
    external fun nDrawSwLottieFrame(lottiePtr: Long, bitmap: Bitmap, frame: Int)

    @JvmStatic
    external fun nDrawGlLottieFrame(lottiePtr: Long, frame: Int)

    @JvmStatic
    external fun nDestroyLottie(lottiePtr: Long)
}
