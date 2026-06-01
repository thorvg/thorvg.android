package org.thorvg.view.lottie.sw

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.View
import androidx.annotation.FloatRange
import androidx.annotation.RawRes
import org.thorvg.core.lottie.LottieConstants
import org.thorvg.view.ThorVGView
import org.thorvg.view.lottie.LottieListener
import org.thorvg.view.lottie.LottieRenderView

/**
 * Software bitmap renderer used internally by [org.thorvg.view.lottie.LottieView].
 */
internal class LottieSwView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ThorVGView(context, attrs, defStyleAttr, defStyleRes), LottieRenderView {
    override val view: View get() = this

    private var listener: LottieListener? = null

    private var resId = Resources.ID_NULL
    private var repeatCount = 0
    private var repeatMode = LottieConstants.RESTART
    private var autoStart = false
    private var speed = 1f
    private var frameFrom = 0
    private var frameTo: Int? = null

    private val lottieDrawable: LottieDrawable?
        get() = getThorVGDrawable() as? LottieDrawable

    override fun setRawRes(@RawRes resId: Int) {
        if (this.resId != resId) {
            this.resId = resId
            updateLottieDrawable()
        }
    }

    @RawRes
    fun getRawRes(): Int = resId

    override fun setRepeatCount(repeatCount: Int) {
        this.repeatCount = repeatCount
        lottieDrawable?.repeatCount = repeatCount
    }

    fun getRepeatCount(): Int = repeatCount

    override fun setRepeatMode(repeatMode: Int) {
        this.repeatMode = repeatMode
        lottieDrawable?.setRepeatMode(repeatMode)
    }

    fun getRepeatMode(): Int = repeatMode

    override fun setAutoStart(autoStart: Boolean) {
        this.autoStart = autoStart
    }

    fun getAutoStart(): Boolean = autoStart

    override fun setFrameFrom(frameFrom: Int) {
        this.frameFrom = frameFrom
        lottieDrawable?.setFirstFrame(frameFrom)
    }

    fun getFrameFrom(): Int = frameFrom

    override fun setFrameTo(frameTo: Int?) {
        this.frameTo = frameTo
        if (frameTo != null) {
            lottieDrawable?.setLastFrame(frameTo)
        }
    }

    fun getFrameTo(): Int? = frameTo

    override fun setSize(width: Int, height: Int) {
        lottieDrawable?.setSize(width, height)
    }

    override fun startAnimation() {
        lottieDrawable?.start()
    }

    override fun stopAnimation() {
        lottieDrawable?.stop()
    }

    override fun pauseAnimation() {
        lottieDrawable?.pause()
    }

    override fun resumeAnimation() {
        lottieDrawable?.resume()
    }

    override fun isAnimating(): Boolean {
        return lottieDrawable?.isRunning == true
    }

    override fun getCurrentFrame(): Int {
        return lottieDrawable?.currentFrame ?: 0
    }

    override fun setSpeed(@FloatRange(from = 0.0) speed: Float) {
        this.speed = speed
        lottieDrawable?.speed = speed
    }

    override fun getSpeed(): Float {
        return lottieDrawable?.speed ?: speed
    }

    override fun setAnimationListener(listener: LottieListener?) {
        this.listener = listener
        lottieDrawable?.setAnimationListener(listener)
    }

    override fun release() {
        clearThorVGDrawable()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (lottieDrawable == null && resId != Resources.ID_NULL) {
            updateLottieDrawable()
        }
    }

    private fun updateLottieDrawable() {
        if (!isAttachedToWindow) return

        val drawable = if (resId != Resources.ID_NULL) {
            LottieDrawable.fromRawResource(context.resources, resId).also(::applyConfig)
        } else {
            null
        }
        setThorVGDrawable(drawable)
        if (drawable != null && autoStart) {
            startAnimation()
        }
    }

    private fun applyConfig(drawable: LottieDrawable) {
        drawable.setAnimationListener(listener)
        drawable.repeatCount = repeatCount
        drawable.setRepeatMode(repeatMode)
        drawable.speed = speed
        drawable.setFirstFrame(frameFrom)
        frameTo?.let(drawable::setLastFrame)
    }
}
