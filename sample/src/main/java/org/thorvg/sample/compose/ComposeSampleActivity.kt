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

package org.thorvg.sample.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.thorvg.lottie.LottieConstants
import org.thorvg.lottie.compose.ThorvgLottie
import org.thorvg.lottie.compose.ThorvgLottieState
import org.thorvg.lottie.compose.rememberThorvgLottieState
import org.thorvg.sample.R

class ComposeSampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ComposeSampleScreen(onNavigateUp = ::finish)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ComposeSampleScreen(onNavigateUp: () -> Unit) {
    val lottieState = rememberThorvgLottieState(
        isPlaying = true,
        repeatCount = LottieConstants.INFINITE
    )

    Scaffold(
        containerColor = Color(0xFFF6F0E8),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sample_compose_title)) },
                navigationIcon = {
                    TextButton(onClick = onNavigateUp) {
                        Text(stringResource(R.string.sample_navigate_up))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF6F0E8))
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ThorvgLottie(
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
}

@Composable
private fun ComposeStatusPanel(state: ThorvgLottieState) {
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
    state: ThorvgLottieState,
    speed: Float
) {
    Button(onClick = { state.speed = speed }) {
        Text(label)
    }
}
