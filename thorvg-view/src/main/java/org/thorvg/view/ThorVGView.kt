package org.thorvg.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View

/**
 * Common View host for ThorVG-backed drawables.
 */
open class ThorVGView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {
    private var thorvgDrawable: ThorVGDrawable? = null

    fun setThorVGDrawable(drawable: ThorVGDrawable?) {
        if (thorvgDrawable === drawable) return

        thorvgDrawable?.callback = null
        thorvgDrawable?.release()

        thorvgDrawable = drawable?.also {
            it.callback = this
            if (measuredWidth > 0 && measuredHeight > 0) {
                it.setSize(measuredWidth, measuredHeight)
            }
        }

        invalidate()
    }

    fun getThorVGDrawable(): ThorVGDrawable? = thorvgDrawable

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        thorvgDrawable?.setSize(measuredWidth, measuredHeight)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        thorvgDrawable?.callback = null
        thorvgDrawable?.release()
        thorvgDrawable = null
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        thorvgDrawable?.draw(canvas)
    }

    override fun invalidateDrawable(drawable: Drawable) {
        super.invalidateDrawable(drawable)
        invalidate()
    }
}
