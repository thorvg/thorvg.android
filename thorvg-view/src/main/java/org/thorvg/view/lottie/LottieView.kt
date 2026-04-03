package org.thorvg.view.lottie

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import androidx.annotation.FloatRange
import androidx.annotation.RawRes
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
    fun setRawRes(@RawRes resId: Int) {
        if (this.resId != resId) {
            this.resId = resId
            updateLottieDrawable()
        }
    }

    /**
     * Returns the configured raw resource id.
     */
    @RawRes
    fun getRawRes(): Int = resId

    /**
     * Updates how many times playback repeats after its first pass.
     */
    fun setRepeatCount(repeatCount: Int) {
        this.repeatCount = repeatCount
        lottieDrawable?.repeatCount = repeatCount
    }

    /**
     * Returns the configured repeat count.
     */
    fun getRepeatCount(): Int = repeatCount

    /**
     * Updates the repeat mode.
     */
    fun setRepeatMode(repeatMode: Int) {
        this.repeatMode = repeatMode
        lottieDrawable?.setRepeatMode(repeatMode)
    }

    /**
     * Returns the configured repeat mode.
     */
    fun getRepeatMode(): Int = repeatMode

    /**
     * Controls whether playback starts automatically after attach.
     */
    fun setAutoStart(autoStart: Boolean) {
        this.autoStart = autoStart
    }

    /**
     * Returns whether playback is configured to auto-start after attach.
     */
    fun getAutoStart(): Boolean = autoStart

    /**
     * Updates the first frame used for playback.
     */
    fun setFrameFrom(frameFrom: Int) {
        this.frameFrom = frameFrom
        lottieDrawable?.setFirstFrame(frameFrom)
    }

    /**
     * Returns the configured first frame.
     */
    fun getFrameFrom(): Int = frameFrom

    /**
     * Updates the last frame used for playback.
     */
    fun setFrameTo(frameTo: Int?) {
        this.frameTo = frameTo
        if (frameTo != null) {
            lottieDrawable?.setLastFrame(frameTo)
        }
    }

    /**
     * Returns the configured last frame when one was explicitly set.
     */
    fun getFrameTo(): Int? = frameTo

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
        if (lottieDrawable == null && resId != Resources.ID_NULL) {
            updateLottieDrawable()
        }
    }

    /**
     * Registers a listener for playback lifecycle callbacks.
     */
    fun setAnimationListener(listener: LottieListener?) {
        this.listener = listener
        lottieDrawable?.setAnimationListener(listener)
    }
}
