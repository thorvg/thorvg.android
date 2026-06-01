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

package org.thorvg.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.thorvg.sample.view.ViewSampleActivity
import org.thorvg.sample.view.ViewSampleType

internal enum class MainScreen {
    Home,
    Compose
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf(MainScreen.Home) }
                    var sampleType by remember { mutableStateOf(SampleType.Lottie) }

                    AnimatedContent(
                        targetState = currentScreen,
                        label = "main_screen_transition",
                        transitionSpec = {
                            val duration = 280
                            when {
                                initialState == MainScreen.Home && targetState == MainScreen.Compose -> {
                                    ContentTransform(
                                        targetContentEnter = slideInHorizontally(
                                            animationSpec = tween(durationMillis = duration),
                                            initialOffsetX = { fullWidth -> fullWidth }
                                        ),
                                        initialContentExit = slideOutHorizontally(
                                            animationSpec = tween(durationMillis = duration),
                                            targetOffsetX = { fullWidth -> -fullWidth / 3 }
                                        )
                                    )
                                }

                                initialState == MainScreen.Compose && targetState == MainScreen.Home -> {
                                    ContentTransform(
                                        targetContentEnter = slideInHorizontally(
                                            animationSpec = tween(durationMillis = duration),
                                            initialOffsetX = { fullWidth -> -fullWidth / 3 }
                                        ),
                                        initialContentExit = slideOutHorizontally(
                                            animationSpec = tween(durationMillis = duration),
                                            targetOffsetX = { fullWidth -> fullWidth }
                                        )
                                    )
                                }

                                else -> {
                                    ContentTransform(
                                        targetContentEnter = slideInHorizontally(
                                            animationSpec = tween(durationMillis = duration),
                                            initialOffsetX = { 0 }
                                        ),
                                        initialContentExit = slideOutHorizontally(
                                            animationSpec = tween(durationMillis = duration),
                                            targetOffsetX = { 0 }
                                        )
                                    )
                                }
                            }
                        }
                    ) { screen ->
                        when (screen) {
                            MainScreen.Home -> {
                                HomeScreen(
                                    onOpenCompose = {
                                        sampleType = SampleType.Lottie
                                        currentScreen = MainScreen.Compose
                                    },
                                    onOpenSvg = {
                                        sampleType = SampleType.Svg
                                        currentScreen = MainScreen.Compose
                                    },
                                    onOpenView = {
                                        startActivity(
                                            ViewSampleActivity.createIntent(
                                                this@MainActivity,
                                                ViewSampleType.Lottie
                                            )
                                        )
                                    },
                                    onOpenPlaybackView = {
                                        startActivity(
                                            ViewSampleActivity.createIntent(
                                                this@MainActivity,
                                                ViewSampleType.LottiePlayback
                                            )
                                        )
                                    },
                                    onOpenSvgView = {
                                        startActivity(
                                            ViewSampleActivity.createIntent(
                                                this@MainActivity,
                                                ViewSampleType.Svg
                                            )
                                        )
                                    }
                                )
                            }

                            MainScreen.Compose -> {
                                ComposeSampleScreen(
                                    sampleType = sampleType,
                                    onNavigateUp = {
                                        currentScreen = MainScreen.Home
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    onOpenCompose: () -> Unit,
    onOpenSvg: () -> Unit,
    onOpenView: () -> Unit,
    onOpenPlaybackView: () -> Unit,
    onOpenSvgView: () -> Unit
) {
    SampleMenuLayout(
        title = stringResource(R.string.sample_menu_title),
        subtitle = stringResource(R.string.sample_menu_subtitle)
    ) {
        MenuButton(
            text = stringResource(R.string.sample_open_compose),
            onClick = onOpenCompose
        )
        MenuButton(
            text = stringResource(R.string.sample_open_svg),
            onClick = onOpenSvg
        )
        MenuButton(
            text = stringResource(R.string.sample_open_view),
            onClick = onOpenView
        )
        MenuButton(
            text = stringResource(R.string.sample_open_svg_view),
            onClick = onOpenSvgView
        )
        ProfilerMenuButton(
            text = stringResource(R.string.sample_open_playback_view),
            onClick = onOpenPlaybackView
        )
    }
}

@Composable
private fun SampleMenuLayout(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3EFE7))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF5B5247)
        )
        content()
    }
}

@Composable
private fun MenuButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text)
    }
}

@Composable
private fun ProfilerMenuButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF5B5247),
            contentColor = Color(0xFFF3EFE7)
        )
    ) {
        Text(text)
    }
}
