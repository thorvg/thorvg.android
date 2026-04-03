package org.thorvg.sample.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.thorvg.compose.lottie.Lottie
import org.thorvg.compose.lottie.LottieState
import org.thorvg.compose.lottie.rememberLottieState
import org.thorvg.core.lottie.LottieConstants
import org.thorvg.sample.R

@Composable
fun LottieComposeSampleContent(
    modifier: Modifier = Modifier
) {
    val lottieState = rememberLottieState(
        isPlaying = true,
        repeatCount = LottieConstants.INFINITE
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Lottie(
            resId = R.raw.swinging,
            state = lottieState,
            modifier = Modifier
                .size(220.dp)
                .background(Color.White)
        )

        ComposeStatusPanel(state = lottieState)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            Button(onClick = {
                if (lottieState.isPlaying) {
                    lottieState.pause()
                } else {
                    lottieState.resume()
                }
            }) {
                Text(
                    if (lottieState.isPlaying) {
                        stringResource(R.string.sample_pause)
                    } else {
                        stringResource(R.string.sample_resume)
                    }
                )
            }

            Button(onClick = { lottieState.stop() }) {
                Text(stringResource(R.string.sample_stop))
            }

            Button(onClick = { lottieState.play() }) {
                Text(stringResource(R.string.sample_replay))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            ComposeSpeedButton(label = "0.5x", state = lottieState, speed = 0.5f)
            ComposeSpeedButton(label = "1x", state = lottieState, speed = 1f)
            ComposeSpeedButton(label = "2x", state = lottieState, speed = 2f)
        }
    }
}

@Composable
private fun ComposeStatusPanel(state: LottieState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (state.isRunning) "Running" else "Idle",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Frame ${state.currentFrame}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Speed ${state.speed}x",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ComposeSpeedButton(
    label: String,
    state: LottieState,
    speed: Float
) {
    Button(onClick = { state.speed = speed }) {
        Text(label)
    }
}
