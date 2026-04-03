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

    fun setRawRes(@RawRes resId: Int) {
        if (rawResId == resId && assetName == null && uri == null) return

        rawResId = resId
        assetName = null
        uri = null
        updateSvgDrawable()
    }

    @RawRes
    fun getRawRes(): Int = rawResId

    fun setAssetName(assetName: String) {
        if (this.assetName == assetName && rawResId == Resources.ID_NULL && uri == null) return

        this.assetName = assetName
        rawResId = Resources.ID_NULL
        uri = null
        updateSvgDrawable()
    }

    fun getAssetName(): String? = assetName

    fun setUri(uri: Uri) {
        if (this.uri == uri && rawResId == Resources.ID_NULL && assetName == null) return

        rawResId = Resources.ID_NULL
        assetName = null
        this.uri = uri
        updateSvgDrawable()
    }

    fun getUri(): Uri? = uri

    fun clearSource() {
        if (rawResId == Resources.ID_NULL && assetName == null && uri == null) return

        rawResId = Resources.ID_NULL
        assetName = null
        uri = null
        clearThorVGDrawable()
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
        if (getThorVGDrawable() == null) {
            updateSvgDrawable()
        }
    }
}
