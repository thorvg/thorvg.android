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

package org.thorvg.lottie

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DrawableRes
import org.thorvg.lottie.LottieDrawable.LottieAnimationListener

class LottieAnimationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {
    private var drawable: LottieDrawable? = null
    private var listener: LottieAnimationListener? = null

    private var resId = Resources.ID_NULL

    private var onAttached = false

    init {
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.LottieAnimationView, defStyleAttr, defStyleRes
        )
        resId = a.getResourceId(R.styleable.LottieAnimationView_lottieDrawable, resId)
        a.recycle()
    }

    fun setLottieDrawableResource(@DrawableRes resId: Int) {
        if (this@LottieAnimationView.resId != resId) {
            this@LottieAnimationView.resId = resId

            drawable?.release()

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

    fun setSize(width: Int, height: Int) {
        drawable?.setSize(width, height)
    }

    fun startAnimation() {
        drawable?.start()
    }

    fun stopAnimation() {
        drawable?.stop()
    }

    fun pauseAnimation() {
        drawable?.pause()
    }

    fun resumeAnimation() {
        drawable?.resume()
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

    fun setAnimationListener(listener: LottieAnimationListener?) {
        this@LottieAnimationView.listener = listener
    }

    override fun onSaveInstanceState(): Parcelable? {
        return super.onSaveInstanceState()
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
    }
}