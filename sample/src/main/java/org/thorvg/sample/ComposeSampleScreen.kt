package org.thorvg.sample

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.thorvg.sample.compose.LottieComposeSampleContent
import org.thorvg.sample.compose.SvgComposeSampleContent
import org.thorvg.sample.multi.LottieMultiSampleContent

@Composable
internal fun ComposeSampleScreen(
    sampleType: SampleType,
    onNavigateUp: () -> Unit
) {
    BackHandler(onBack = onNavigateUp)

    when (sampleType) {
        // Manages its own chrome: top bar on the config form, fullscreen + immersive while running.
        SampleType.LottieMulti -> LottieMultiSampleContent(onNavigateUp = onNavigateUp)
        SampleType.Lottie -> FramedSample(R.string.sample_compose_title, onNavigateUp) {
            LottieComposeSampleContent(modifier = it)
        }
        SampleType.Svg -> FramedSample(R.string.sample_svg_title, onNavigateUp) {
            SvgComposeSampleContent(modifier = it)
        }
    }
}

/** A sample shown inside a top-bar Scaffold; [content] receives the padded content modifier. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FramedSample(
    @StringRes titleRes: Int,
    onNavigateUp: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        containerColor = Color(0xFFF6F0E8),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.sample_navigate_up)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        content(
            Modifier
                .fillMaxSize()
                .background(Color(0xFFF6F0E8))
                .padding(innerPadding)
                .padding(24.dp)
        )
    }
}
