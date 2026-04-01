/*
 * Copyright (c) 2025 - 2026 ThorVG project. All rights reserved.

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

package org.thorvg.lottie.view

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import org.thorvg.lottie.LottieAnimationListener

/**
 * View-based host for rendering a [LottieDrawable] inside the Android View system.
 */
class LottieAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {
    private var drawable: LottieDrawable? = null
    private var listener: LottieAnimationListener? = null

    private var resId = Resources.ID_NULL
    private var onAttached = false

    init {
        val attributes = context.obtainStyledAttributes(
            attrs,
            R.styleable.LottieAnimationView,
            defStyleAttr,
            defStyleRes
        )
        resId = attributes.getResourceId(R.styleable.LottieAnimationView_lottieDrawable, resId)
        attributes.recycle()
    }

    /**
     * Replaces the currently bound drawable resource.
     */
    fun setLottieDrawableResource(@DrawableRes resId: Int) {
        if (this.resId != resId) {
            this.resId = resId
            drawable?.release()
            drawable = null
            createLottieDrawable()
        }
    }

    private fun createLottieDrawable() {
        if (onAttached && resId != Resources.ID_NULL && drawable == null) {
            drawable = LottieDrawable.create(context.resources, resId)
            drawable?.callback = this
            drawable?.setAnimationListener(listener)
            if (drawable?.isAutoPlay == true) {
                startAnimation()
            }
        }
    }

    /**
     * Resizes the underlying drawable buffer in pixels.
     */
    fun setSize(width: Int, height: Int) {
        drawable?.setSize(width, height)
    }

    /**
     * Starts playback from the first configured frame.
     */
    fun startAnimation() {
        drawable?.start()
    }

    /**
     * Stops playback and clears scheduled frame updates.
     */
    fun stopAnimation() {
        drawable?.stop()
    }

    /**
     * Pauses playback without resetting the current frame.
     */
    fun pauseAnimation() {
        drawable?.pause()
    }

    /**
     * Resumes playback from the current frame.
     */
    fun resumeAnimation() {
        drawable?.resume()
    }

    /**
     * Returns whether the underlying drawable is actively playing.
     */
    fun isAnimating(): Boolean {
        return drawable?.isRunning == true
    }

    /**
     * Returns the current frame index.
     */
    fun getCurrentFrame(): Int {
        return drawable?.currentFrame ?: 0
    }

    /**
     * Updates the playback speed multiplier.
     */
    fun setSpeed(@FloatRange(from = 0.0) speed: Float) {
        drawable?.speed = speed
    }

    /**
     * Returns the playback speed multiplier.
     */
    fun getSpeed(): Float {
        return drawable?.speed ?: 1f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        drawable?.setSize(measuredWidth, measuredHeight)
    }

    override fun onAttachedToWindow() {
        onAttached = true
        super.onAttachedToWindow()
        createLottieDrawable()
    }

    override fun onDetachedFromWindow() {
        onAttached = false
        super.onDetachedFromWindow()
        drawable?.release()
        drawable = null
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        drawable?.draw(canvas)
    }

    override fun invalidateDrawable(drawable: Drawable) {
        super.invalidateDrawable(drawable)
        invalidate()
    }

    /**
     * Registers a listener for playback lifecycle callbacks.
     */
    fun setAnimationListener(listener: LottieAnimationListener?) {
        this.listener = listener
    }

    override fun onSaveInstanceState(): Parcelable? {
        return super.onSaveInstanceState()
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
    }
}
