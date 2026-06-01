package org.thorvg.view.lottie

import android.view.View
import androidx.annotation.RawRes

internal enum class LottieRenderViewType {
    Sw,
    Gl
}

internal interface LottieRenderView {
    val view: View

    fun setRawRes(@RawRes resId: Int)
    fun setRepeatCount(repeatCount: Int)
    fun setRepeatMode(repeatMode: Int)
    fun setAutoStart(autoStart: Boolean)
    fun setFrameFrom(frameFrom: Int)
    fun setFrameTo(frameTo: Int?)
    fun setSize(width: Int, height: Int)
    fun startAnimation()
    fun stopAnimation()
    fun pauseAnimation()
    fun resumeAnimation()
    fun isAnimating(): Boolean
    fun getCurrentFrame(): Int
    fun setSpeed(speed: Float)
    fun getSpeed(): Float
    fun setAnimationListener(listener: LottieListener?)
    fun release()
}
