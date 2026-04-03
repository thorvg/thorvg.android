package org.thorvg.view.svg

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.util.AttributeSet
import androidx.annotation.RawRes
import org.thorvg.view.R
import org.thorvg.view.ThorVGView

/**
 * View-based host for rendering a [SvgDrawable] inside the Android View system.
 */
class SvgView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ThorVGView(context, attrs, defStyleAttr, defStyleRes) {
    private var rawResId = Resources.ID_NULL
    private var assetName: String? = null
    private var uri: Uri? = null

    init {
        val attributes = context.obtainStyledAttributes(
            attrs,
            R.styleable.SvgView,
            defStyleAttr,
            defStyleRes
        )
        rawResId = attributes.getResourceId(R.styleable.SvgView_rawRes, rawResId)
        assetName = attributes.getString(R.styleable.SvgView_assetName)
        attributes.recycle()
    }

    fun setSvgRawResource(@RawRes resId: Int) {
        rawResId = resId
        assetName = null
        uri = null
        updateSvgDrawable()
    }

    fun setSvgAsset(assetName: String) {
        this.assetName = assetName
        rawResId = Resources.ID_NULL
        uri = null
        updateSvgDrawable()
    }

    fun setSvgUri(uri: Uri) {
        rawResId = Resources.ID_NULL
        assetName = null
        this.uri = uri
        updateSvgDrawable()
    }

    private fun updateSvgDrawable() {
        if (!isAttachedToWindow) return

        val drawable = when {
            rawResId != Resources.ID_NULL -> SvgDrawable.fromRawResource(resources, rawResId)
            !assetName.isNullOrBlank() -> SvgDrawable.fromAsset(context, requireNotNull(assetName))
            uri != null -> SvgDrawable.fromUri(context, requireNotNull(uri))
            else -> null
        }
        setThorVGDrawable(drawable)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateSvgDrawable()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }
}
