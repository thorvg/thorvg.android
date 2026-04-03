package org.thorvg.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View

/**
 * Common View host for ThorVG-backed drawables.
 */
abstract class ThorVGView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {
    private var thorvgDrawable: ThorVGDrawable? = null

    protected fun setThorVGDrawable(drawable: ThorVGDrawable?) {
        if (thorvgDrawable === drawable) return

        clearDrawableCallback()
        releaseDrawableIfNeeded(drawable)

        thorvgDrawable = drawable?.also {
            bindDrawable(it)
        }

        invalidate()
    }

    protected fun getThorVGDrawable(): ThorVGDrawable? = thorvgDrawable

    protected fun clearThorVGDrawable() {
        clearDrawableCallback()
        thorvgDrawable?.release()
        thorvgDrawable = null
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        thorvgDrawable?.setSize(measuredWidth, measuredHeight)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        thorvgDrawable?.let(::bindDrawable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clearDrawableCallback()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        thorvgDrawable?.draw(canvas)
    }

    override fun invalidateDrawable(drawable: Drawable) {
        super.invalidateDrawable(drawable)
        invalidate()
    }

    private fun bindDrawable(drawable: ThorVGDrawable) {
        drawable.callback = this
        if (measuredWidth > 0 && measuredHeight > 0) {
            drawable.setSize(measuredWidth, measuredHeight)
        }
    }

    private fun clearDrawableCallback() {
        thorvgDrawable?.callback = null
    }

    private fun releaseDrawableIfNeeded(nextDrawable: ThorVGDrawable?) {
        if (thorvgDrawable != null && thorvgDrawable !== nextDrawable) {
            thorvgDrawable?.release()
        }
    }
}
