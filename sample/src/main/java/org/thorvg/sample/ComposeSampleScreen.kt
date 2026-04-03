package org.thorvg.sample

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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ComposeSampleScreen(
    sampleType: SampleType,
    onNavigateUp: () -> Unit
) {
    val titleRes = when (sampleType) {
        SampleType.Lottie -> R.string.sample_compose_title
        SampleType.Svg -> R.string.sample_svg_title
    }

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
        when (sampleType) {
            SampleType.Lottie -> {
                LottieComposeSampleContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF6F0E8))
                        .padding(innerPadding)
                        .padding(24.dp)
                )
            }

            SampleType.Svg -> {
                SvgComposeSampleContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF6F0E8))
                        .padding(innerPadding)
                        .padding(24.dp)
                )
            }
        }
    }
}