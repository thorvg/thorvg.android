package org.thorvg.view.svg

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.net.Uri
import androidx.annotation.RawRes
import org.thorvg.core.svg.SvgComposition
import org.thorvg.view.ThorVGDrawable

/**
 * Drawable adapter that renders a ThorVG SVG into an Android [Canvas].
 */
class SvgDrawable internal constructor(
    private var composition: SvgComposition
) : ThorVGDrawable() {
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)

    private var currentBitmap: Bitmap? = null
    private var viewportWidth = 0
    private var viewportHeight = 0
    private var renderedWidth = 0
    private var renderedHeight = 0
    private var left = 0f
    private var top = 0f

    override fun draw(canvas: Canvas) {
        currentBitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, left, top, paint)
        }
    }

    override fun setAlpha(alpha: Int) = Unit

    @Deprecated("Deprecated in Drawable")
    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Deprecated in Drawable")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth(): Int = composition.width

    override fun getIntrinsicHeight(): Int = composition.height

    override fun setSize(width: Int, height: Int) {
        require(width > 0) { "SvgDrawable requires width > 0" }
        require(height > 0) { "SvgDrawable requires height > 0" }

        if (viewportWidth == width && viewportHeight == height && currentBitmap != null) return

        viewportWidth = width
        viewportHeight = height

        val intrinsicWidth = composition.width
        val intrinsicHeight = composition.height
        val targetWidth: Int
        val targetHeight: Int
        if (intrinsicWidth > 0 && intrinsicHeight > 0) {
            val scale = minOf(
                width.toFloat() / intrinsicWidth,
                height.toFloat() / intrinsicHeight
            )
            targetWidth = (intrinsicWidth * scale).toInt().coerceAtLeast(1)
            targetHeight = (intrinsicHeight * scale).toInt().coerceAtLeast(1)
        } else {
            targetWidth = width
            targetHeight = height
        }

        renderedWidth = targetWidth
        renderedHeight = targetHeight
        left = ((viewportWidth - renderedWidth) / 2f).coerceAtLeast(0f)
        top = ((viewportHeight - renderedHeight) / 2f).coerceAtLeast(0f)

        composition.setSize(renderedWidth, renderedHeight)
        currentBitmap = composition.render()
        invalidateSelf()
    }

    override fun release() {
        composition.release()
        currentBitmap = null
    }

    companion object {
        @JvmStatic
        fun fromRawResource(resources: Resources, @RawRes resId: Int): SvgDrawable {
            return SvgDrawable(SvgComposition.fromRawResource(resources, resId))
        }

        @JvmStatic
        fun fromAsset(context: Context, assetName: String): SvgDrawable {
            return SvgDrawable(SvgComposition.fromAsset(context.assets, assetName))
        }

        @JvmStatic
        fun fromUri(context: Context, uri: Uri): SvgDrawable {
            return SvgDrawable(SvgComposition.fromUri(context, uri))
        }
    }
}
