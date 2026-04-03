package org.thorvg.view.lottie

import android.content.Context
import android.content.res.Resources
import android.os.Parcelable
import android.util.AttributeSet
import androidx.annotation.RawRes
import androidx.annotation.FloatRange
import org.thorvg.core.lottie.LottieConstants
import org.thorvg.view.R
import org.thorvg.view.ThorVGView

/**
 * View-based host for rendering a [LottieDrawable] inside the Android View system.
 */
class LottieView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ThorVGView(context, attrs, defStyleAttr, defStyleRes) {
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

    init {
        val attributes = context.obtainStyledAttributes(
            attrs,
            R.styleable.LottieView,
            defStyleAttr,
            defStyleRes
        )
        resId = attributes.getResourceId(R.styleable.LottieView_rawRes, resId)
        speed = attributes.getFloat(R.styleable.LottieView_speed, speed)
        repeatCount = attributes.getInt(R.styleable.LottieView_android_repeatCount, repeatCount)
        repeatMode = attributes.getInt(R.styleable.LottieView_android_repeatMode, repeatMode)
        autoStart = attributes.getBoolean(R.styleable.LottieView_android_autoStart, autoStart)
        frameFrom = attributes.getInt(R.styleable.LottieView_frameFrom, frameFrom)
        frameTo = if (attributes.hasValue(R.styleable.LottieView_frameTo)) {
            attributes.getInt(R.styleable.LottieView_frameTo, 0)
        } else {
            null
        }
        attributes.recycle()
    }

    /**
     * Replaces the currently bound drawable resource.
     */
    fun setLottieDrawableResource(@RawRes resId: Int) {
        if (this.resId != resId) {
            this.resId = resId
            updateLottieDrawable()
        }
    }

    private fun updateLottieDrawable() {
        if (isAttachedToWindow) {
            val drawable = if (resId != Resources.ID_NULL) {
                LottieDrawable.fromRawResource(context.resources, resId).also {
                    it.setAnimationListener(listener)
                    it.repeatCount = repeatCount
                    it.setRepeatMode(repeatMode)
                    it.speed = speed
                    it.setFirstFrame(frameFrom)
                    frameTo?.let(it::setLastFrame)
                }
            } else {
                null
            }
            setThorVGDrawable(drawable)
            if (drawable != null && autoStart) {
                startAnimation()
            }
        }
    }

    /**
     * Resizes the underlying drawable buffer in pixels.
     */
    fun setSize(width: Int, height: Int) {
        lottieDrawable?.setSize(width, height)
    }

    /**
     * Starts playback from the first configured frame.
     */
    fun startAnimation() {
        lottieDrawable?.start()
    }

    /**
     * Stops playback and clears scheduled frame updates.
     */
    fun stopAnimation() {
        lottieDrawable?.stop()
    }

    /**
     * Pauses playback without resetting the current frame.
     */
    fun pauseAnimation() {
        lottieDrawable?.pause()
    }

    /**
     * Resumes playback from the current frame.
     */
    fun resumeAnimation() {
        lottieDrawable?.resume()
    }

    /**
     * Returns whether the underlying drawable is actively playing.
     */
    fun isAnimating(): Boolean {
        return lottieDrawable?.isRunning == true
    }

    /**
     * Returns the current frame index.
     */
    fun getCurrentFrame(): Int {
        return lottieDrawable?.currentFrame ?: 0
    }

    /**
     * Updates the playback speed multiplier.
     */
    fun setSpeed(@FloatRange(from = 0.0) speed: Float) {
        this.speed = speed
        lottieDrawable?.speed = speed
    }

    /**
     * Returns the playback speed multiplier.
     */
    fun getSpeed(): Float {
        return lottieDrawable?.speed ?: 1f
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateLottieDrawable()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    /**
     * Registers a listener for playback lifecycle callbacks.
     */
    fun setAnimationListener(listener: LottieListener?) {
        this.listener = listener
        lottieDrawable?.setAnimationListener(listener)
    }

    override fun onSaveInstanceState(): Parcelable? {
        return super.onSaveInstanceState()
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
    }
}
