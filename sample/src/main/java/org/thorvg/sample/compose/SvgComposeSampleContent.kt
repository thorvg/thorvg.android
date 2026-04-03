package org.thorvg.sample.compose

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.thorvg.compose.svg.Svg
import org.thorvg.sample.R

private enum class SvgSampleType {
    RAW,
    ASSET,
    URI
}

private val SVG_SAMPLE_TYPE = SvgSampleType.ASSET

@Composable
fun SvgComposeSampleContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resourceUri = Uri.parse("android.resource://${context.packageName}/${R.raw.tiger}")
    val svgSourceLabel = when (SVG_SAMPLE_TYPE) {
        SvgSampleType.URI -> "Svg(uri = android.resource://.../raw/tiger)"
        SvgSampleType.ASSET -> "Svg(assetName = \"thorvg_mono_black.svg\")"
        SvgSampleType.RAW -> "Svg(resId = R.raw.tiger)"
    }
    val svgSourceDescription = when (SVG_SAMPLE_TYPE) {
        SvgSampleType.URI -> "ThorVG Compose Svg() example using android.resource Uri"
        SvgSampleType.ASSET -> "ThorVG Compose Svg() example using assets/thorvg_mono_black.svg"
        SvgSampleType.RAW -> "ThorVG Compose Svg() example using raw/tiger.svg"
    }
    val svgContent: @Composable () -> Unit = when (SVG_SAMPLE_TYPE) {
        SvgSampleType.URI -> {
            {
                Svg(
                    uri = resourceUri,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        SvgSampleType.ASSET -> {
            {
                Svg(
                    assetName = "thorvg_mono_black.svg",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        SvgSampleType.RAW -> {
            {
                Svg(
                    resId = R.raw.tiger,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = svgSourceLabel,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = svgSourceDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF5B5247)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            svgContent()
        }
    }
}
