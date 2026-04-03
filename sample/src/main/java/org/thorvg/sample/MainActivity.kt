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

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.thorvg.sample.compose.LottieComposeSampleActivity
import org.thorvg.sample.compose.SvgSampleActivity
import org.thorvg.sample.view.LottieViewSampleActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainMenu(
                        onOpenCompose = {
                            startActivity(Intent(this, LottieComposeSampleActivity::class.java))
                        },
                        onOpenSvg = {
                            startActivity(Intent(this, SvgSampleActivity::class.java))
                        },
                        onOpenView = {
                            startActivity(Intent(this, LottieViewSampleActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainMenu(
    onOpenCompose: () -> Unit,
    onOpenSvg: () -> Unit,
    onOpenView: () -> Unit
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
            text = stringResource(R.string.sample_menu_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = stringResource(R.string.sample_menu_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF5B5247)
        )

        Button(
            onClick = onOpenCompose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.sample_open_compose))
        }

        Button(
            onClick = onOpenSvg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.sample_open_svg))
        }

        Button(
            onClick = onOpenView,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.sample_open_view))
        }
    }
}
