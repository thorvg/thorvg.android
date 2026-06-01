/*
 * Copyright (c) 2026 ThorVG project. All rights reserved.

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

package org.thorvg.view.lottie.gl

import android.content.Context
import android.content.res.Resources
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.TextureView
import android.view.View
import androidx.annotation.FloatRange
import androidx.annotation.RawRes
import org.thorvg.core.lottie.LottieGlComposition
import org.thorvg.core.lottie.LottieRenderState
import org.thorvg.core.lottie.gl.GlRenderer
import org.thorvg.view.R
import org.thorvg.view.lottie.LottieListener
import org.thorvg.view.lottie.LottieRenderView

/**
 * TextureView-based host for rendering a Lottie composition through ThorVG's GL canvas.
 */
internal class LottieGlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : TextureView(context, attrs, defStyleAttr, defStyleRes),
    TextureView.SurfaceTextureListener,
    LottieRenderView {
    override val view: View get() = this

    private var listener: LottieListener? = null
    private var renderFailureListener: (() -> Unit)? = null
    private var renderer: GlRenderer? = null
    private val renderState = LottieRenderState()
    private var playRequested = false

    private var resId = Resources.ID_NULL
    private var frameFrom = 0
    private var frameTo: Int? = null

    init {
        val attributes = context.obtainStyledAttributes(
            attrs,
            R.styleable.LottieView,
            defStyleAttr,
            defStyleRes
        )
        resId = attributes.getResourceId(R.styleable.LottieView_rawRes, resId)
        renderState.speed = attributes.getFloat(R.styleable.LottieView_speed, renderState.speed)
        renderState.repeatCount = attributes.getInt(R.styleable.LottieView_android_repeatCount, renderState.repeatCount)
        renderState.repeatMode = attributes.getInt(R.styleable.LottieView_android_repeatMode, renderState.repeatMode)
        renderState.autoPlay = attributes.getBoolean(R.styleable.LottieView_android_autoStart, renderState.autoPlay)
        frameFrom = attributes.getInt(R.styleable.LottieView_frameFrom, frameFrom)
        frameTo = if (attributes.hasValue(R.styleable.LottieView_frameTo)) {
            attributes.getInt(R.styleable.LottieView_frameTo, 0)
        } else {
            null
        }
        attributes.recycle()
        isOpaque = false
        surfaceTextureListener = this
    }

    override fun setRawRes(@RawRes resId: Int) {
        if (this.resId != resId) {
            this.resId = resId
            updateComposition()
        }
    }

    @RawRes
    fun getRawRes(): Int = resId

    override fun setRepeatCount(repeatCount: Int) {
        renderState.repeatCount = repeatCount
        renderer?.setConfig(currentState(), frameFrom, frameTo)
    }

    fun getRepeatCount(): Int = renderState.repeatCount

    override fun setRepeatMode(repeatMode: Int) {
        renderState.repeatMode = repeatMode
        renderer?.setConfig(currentState(), frameFrom, frameTo)
    }

    fun getRepeatMode(): Int = renderState.repeatMode

    override fun setAutoStart(autoStart: Boolean) {
        renderState.autoPlay = autoStart
    }

    fun getAutoStart(): Boolean = renderState.autoPlay

    override fun setFrameFrom(frameFrom: Int) {
        this.frameFrom = frameFrom
        renderer?.setConfig(currentState(), frameFrom, frameTo)
    }

    fun getFrameFrom(): Int = frameFrom

    override fun setFrameTo(frameTo: Int?) {
        this.frameTo = frameTo
        renderer?.setConfig(currentState(), frameFrom, frameTo)
    }

    fun getFrameTo(): Int? = frameTo

    override fun startAnimation() {
        playRequested = true
        ensureRenderer().start()
    }

    override fun stopAnimation() {
        playRequested = false
        renderer?.stop()
    }

    override fun pauseAnimation() {
        playRequested = false
        renderer?.pause()
    }

    override fun resumeAnimation() {
        playRequested = true
        ensureRenderer().resume()
    }

    override fun isAnimating(): Boolean {
        return renderer?.isRunning == true
    }

    override fun getCurrentFrame(): Int {
        return renderer?.currentFrame ?: frameFrom.coerceAtLeast(0)
    }

    override fun setSpeed(@FloatRange(from = 0.0) speed: Float) {
        renderState.speed = speed
        renderer?.setConfig(currentState(), frameFrom, frameTo)
    }

    override fun getSpeed(): Float = renderState.speed

    override fun setAnimationListener(listener: LottieListener?) {
        this.listener = listener
    }

    fun setRenderFailureListener(listener: (() -> Unit)?) {
        renderFailureListener = listener
    }

    override fun setSize(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            renderer?.resize(width, height)
        }
    }

    override fun release() {
        renderer?.release()
        renderer = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ensureRenderer()
        updateComposition()
        if (isAvailable) {
            surfaceTexture?.let {
                updateDefaultBufferSize(it, width, height)
                renderer?.setSurface(it, width, height)
            }
        }
        if (playRequested) {
            renderer?.start()
        }
    }

    override fun onDetachedFromWindow() {
        release()
        super.onDetachedFromWindow()
    }

    // interface SurfaceTextureListener
    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        updateDefaultBufferSize(surface, width, height)
        ensureRenderer().setSurface(surface, width, height)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        updateDefaultBufferSize(surface, width, height)
        renderer?.resize(width, height)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        renderer?.clearSurface()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    private fun updateDefaultBufferSize(surface: SurfaceTexture, width: Int, height: Int) {
        if (width > 0 && height > 0) {
            surface.setDefaultBufferSize(width, height)
        }
    }

    private fun updateComposition() {
        if (!isAttachedToWindow) return

        val compositionFactory = if (resId != Resources.ID_NULL) {
            val resources = context.resources
            val rawResId = resId
            { LottieGlComposition.fromRawResource(resources, rawResId) }
        } else {
            null
        }
        ensureRenderer().setCompositionFactory(compositionFactory, currentState(), frameFrom, frameTo)
    }

    private fun ensureRenderer(): GlRenderer {
        return renderer ?: GlRenderer(
            onAnimationStart = { listener?.onAnimationStart() },
            onAnimationEnd = { listener?.onAnimationEnd() },
            onAnimationRepeat = { listener?.onAnimationRepeat() },
            onRenderFailure = { renderFailureListener?.invoke() }
        ).also { renderer = it }
    }

    private fun currentState(): LottieRenderState {
        return LottieRenderState().also { renderState.copyPlaybackTo(it) }
    }
}
