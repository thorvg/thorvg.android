package org.thorvg.core.video

import android.graphics.Bitmap

object VideoNativeBindings {
    init {
        System.loadLibrary("lottie-libs")
    }

    @JvmStatic
    external fun nCreateVideo(path: String?, outValues: IntArray?): Long

    @JvmStatic
    external fun nSetVideoBufferSize(
        videoPtr: Long,
        bitmap: Bitmap,
        width: Float,
        height: Float
    )

    @JvmStatic
    external fun nDrawVideo(videoPtr: Long, bitmap: Bitmap)

    @JvmStatic
    external fun nPlayVideo(videoPtr: Long)

    @JvmStatic
    external fun nPauseVideo(videoPtr: Long)

    @JvmStatic
    external fun nStopVideo(videoPtr: Long)

    @JvmStatic
    external fun nSeekVideo(videoPtr: Long, timeMillis: Long)

    @JvmStatic
    external fun nLoopVideo(videoPtr: Long, loop: Boolean)

    @JvmStatic
    external fun nSetVideoVolume(videoPtr: Long, volume: Float)

    @JvmStatic
    external fun nSetVideoMuted(videoPtr: Long, muted: Boolean)

    @JvmStatic
    external fun nGetVideoTime(videoPtr: Long): Long

    @JvmStatic
    external fun nDestroyVideo(videoPtr: Long)
}
