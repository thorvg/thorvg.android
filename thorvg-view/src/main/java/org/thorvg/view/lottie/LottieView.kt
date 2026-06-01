package org.thorvg.view.lottie

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.FloatRange
import androidx.annotation.RawRes
import org.thorvg.core.lottie.LottieConstants
import org.thorvg.view.R
import org.thorvg.view.lottie.gl.LottieGlView
import org.thorvg.view.lottie.sw.LottieSwView

/**
 * View-based host for rendering Lottie with a selectable ThorVG renderer.
 */
class LottieView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
    private var renderView: LottieRenderView? = null
    private var listener: LottieListener? = null

    // attributes
    private var resId = Resources.ID_NULL
    private var repeatCount = 0
    private var repeatMode = LottieConstants.RESTART
    private var autoStart = false
    private var speed = 1f
    private var frameFrom = 0
    private var frameTo: Int? = null
    private var renderer = Renderer.Default

    private var glRenderFailed = false

    init {
        val attributes = context.obtainStyledAttributes(
            attrs,
            R.styleable.LottieView,
            defStyleAttr,
            defStyleRes
        )
        resId = attributes.getResourceId(R.styleable.LottieView_rawRes, resId)
        repeatCount = attributes.getInt(R.styleable.LottieView_android_repeatCount, repeatCount)
        repeatMode = attributes.getInt(R.styleable.LottieView_android_repeatMode, repeatMode)
        autoStart = attributes.getBoolean(R.styleable.LottieView_android_autoStart, autoStart)
        speed = attributes.getFloat(R.styleable.LottieView_speed, speed)
        frameFrom = attributes.getInt(R.styleable.LottieView_frameFrom, frameFrom)
        frameTo = if (attributes.hasValue(R.styleable.LottieView_frameTo)) {
            attributes.getInt(R.styleable.LottieView_frameTo, 0)
        } else {
            null
        }
        renderer = Renderer.fromAttr(
            attributes.getInt(R.styleable.LottieView_renderer, Renderer.Default.attrValue)
        )
        attributes.recycle()

        installRenderView(startAfterInstall = false)
    }

    fun setRenderer(renderer: Renderer) {
        if (this.renderer == renderer) return

        val wasAnimating = isAnimating()
        this.renderer = renderer
        glRenderFailed = false
        installRenderView(startAfterInstall = wasAnimating)
    }

    fun getRenderer(): Renderer = renderer

    /**
     * Replaces the currently bound drawable resource.
     */
    fun setRawRes(@RawRes resId: Int) {
        if (this.resId != resId) {
            this.resId = resId
            renderView?.setRawRes(resId)
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
        renderView?.setRepeatCount(repeatCount)
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
        renderView?.setRepeatMode(repeatMode)
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
        renderView?.setAutoStart(autoStart)
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
        renderView?.setFrameFrom(frameFrom)
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
        renderView?.setFrameTo(frameTo)
    }

    /**
     * Returns the configured last frame when one was explicitly set.
     */
    fun getFrameTo(): Int? = frameTo

    /**
     * Resizes the underlying rendering target in pixels.
     */
    fun setSize(width: Int, height: Int) {
        renderView?.setSize(width, height)
    }

    /**
     * Starts playback from the first configured frame.
     */
    fun startAnimation() {
        renderView?.startAnimation()
    }

    /**
     * Stops playback and clears scheduled frame updates.
     */
    fun stopAnimation() {
        renderView?.stopAnimation()
    }

    /**
     * Pauses playback without resetting the current frame.
     */
    fun pauseAnimation() {
        renderView?.pauseAnimation()
    }

    /**
     * Resumes playback from the current frame.
     */
    fun resumeAnimation() {
        renderView?.resumeAnimation()
    }

    /**
     * Returns whether the active renderer is playing.
     */
    fun isAnimating(): Boolean {
        return renderView?.isAnimating() == true
    }

    /**
     * Returns the current frame index.
     */
    fun getCurrentFrame(): Int {
        return renderView?.getCurrentFrame() ?: 0
    }

    /**
     * Updates the playback speed multiplier.
     */
    fun setSpeed(@FloatRange(from = 0.0) speed: Float) {
        this.speed = speed
        renderView?.setSpeed(speed)
    }

    /**
     * Returns the playback speed multiplier.
     */
    fun getSpeed(): Float {
        return renderView?.getSpeed() ?: speed
    }

    /**
     * Registers a listener for playback lifecycle callbacks.
     */
    fun setAnimationListener(listener: LottieListener?) {
        this.listener = listener
        renderView?.setAnimationListener(listener)
    }

    private fun installRenderView(startAfterInstall: Boolean): LottieRenderView {
        return installRenderView(renderViewTypeFor(renderer), startAfterInstall)
    }

    private fun installRenderView(
        renderViewType: LottieRenderViewType,
        startAfterInstall: Boolean
    ): LottieRenderView {
        val previousRenderView = renderView
        previousRenderView?.stopAnimation()
        previousRenderView?.release()
        previousRenderView?.view?.let { removeView(it) }

        val nextRenderView = when (renderViewType) {
            LottieRenderViewType.Sw -> LottieSwView(context)
            LottieRenderViewType.Gl -> createGlRenderView()
        }
        renderView = nextRenderView
        addView(
            nextRenderView.view,
            0,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        applyConfig(nextRenderView)
        if (startAfterInstall && isAttachedToWindow) {
            nextRenderView.startAnimation()
        }
        return nextRenderView
    }

    private fun createGlRenderView(): LottieGlView {
        val glView = LottieGlView(context)
        glView.setRenderFailureListener { fallbackToSoftware(glView) }
        return glView
    }

    private fun fallbackToSoftware(failedRenderView: LottieRenderView) {
        if (renderView !== failedRenderView || glRenderFailed) return

        val wasAnimating = failedRenderView.isAnimating()
        glRenderFailed = true
        installRenderView(LottieRenderViewType.Sw, startAfterInstall = wasAnimating)
    }

    private fun applyConfig(renderView: LottieRenderView) {
        renderView.setAnimationListener(listener)
        renderView.setRepeatCount(repeatCount)
        renderView.setRepeatMode(repeatMode)
        renderView.setAutoStart(autoStart)
        renderView.setFrameFrom(frameFrom)
        renderView.setFrameTo(frameTo)
        renderView.setSpeed(speed)
        renderView.setRawRes(resId)
    }

    private fun renderViewTypeFor(renderer: Renderer): LottieRenderViewType {
        return when (renderer) {
            Renderer.Default ->
                if (glRenderFailed) LottieRenderViewType.Sw else LottieRenderViewType.Gl
            Renderer.Sw -> LottieRenderViewType.Sw
            Renderer.Gl ->
                if (glRenderFailed) LottieRenderViewType.Sw else LottieRenderViewType.Gl
        }
    }
}
