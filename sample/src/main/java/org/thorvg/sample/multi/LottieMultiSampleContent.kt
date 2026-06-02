/*
 * Copyright (c) 2026 ThorVG project. All rights reserved.

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

package org.thorvg.sample.multi

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import androidx.activity.compose.BackHandler
import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import org.json.JSONObject
import org.thorvg.compose.lottie.LottieRenderer
import org.thorvg.sample.R
import org.thorvg.view.lottie.Renderer

/** Config form (with a top bar) ↔ fullscreen run screen. The last config prefills the form on return. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun LottieMultiSampleContent(onNavigateUp: () -> Unit) {
    var running by remember { mutableStateOf<RunConfig?>(null) }
    var lastConfig by remember { mutableStateOf<RunConfig?>(null) }

    val config = running
    if (config == null) {
        Scaffold(
            containerColor = FORM_BACKGROUND,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.sample_multi_title)) },
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
            ConfigForm(initial = lastConfig, modifier = Modifier.padding(innerPadding)) {
                lastConfig = it
                running = it
            }
        }
    } else {
        // Run renders fullscreen (no top bar) and goes immersive — see RunScreen.
        RunScreen(config) { running = null }
    }
}

// region Config form

@Composable
private fun ConfigForm(initial: RunConfig?, modifier: Modifier = Modifier, onStart: (RunConfig) -> Unit) {
    var host by remember { mutableStateOf(initial?.host ?: Host.View) }
    var renderer by remember { mutableStateOf(initial?.renderer ?: Renderer.Sw) }
    var sizeMode by remember { mutableStateOf(initial?.sizeMode ?: SizeMode.Fit) }
    var cellsPerRow by remember { mutableStateOf(initial?.cellsPerRow ?: DEFAULT_CELLS_PER_ROW) }
    var durationSec by remember {
        mutableStateOf(((initial?.durationMillis ?: DEFAULT_DURATION_MILLIS) / 1000L).toString())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EnumSelector("Host", Host.entries, host, Modifier.weight(1f)) { host = it }
            EnumSelector("Renderer", listOf(Renderer.Sw, Renderer.Gl), renderer, Modifier.weight(1f), Renderer::label) { renderer = it }
            EnumSelector("Size", SizeMode.entries, sizeMode, Modifier.weight(1f)) { sizeMode = it }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            DropdownField("Cells per row", CELLS_PER_ROW_OPTIONS, cellsPerRow, Modifier.weight(1f)) { cellsPerRow = it }
            NumberField("Duration sec", durationSec, Modifier.weight(1f)) { durationSec = it }
        }
        Button(
            onClick = {
                onStart(
                    RunConfig(
                        host = host,
                        renderer = renderer,
                        cellsPerRow = cellsPerRow,
                        sizeMode = sizeMode,
                        durationMillis = durationSec.toBoundedIntOr(DEFAULT_DURATION_SECONDS, MAX_DURATION_SECONDS) * 1000L
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) { Text("Start") }
    }
}

@Composable
private fun <T> EnumSelector(
    title: String,
    options: List<T>,
    selected: T,
    modifier: Modifier = Modifier,
    label: (T) -> String = { it.toString() },
    onSelect: (T) -> Unit
) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.labelMedium)
        options.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onSelect(option) }
            ) {
                RadioButton(selected = option == selected, onClick = { onSelect(option) })
                Text(label(option))
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun <T> DropdownField(
    label: String,
    options: List<T>,
    selected: T,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected.toString())
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.toString()) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// endregion

// region Run screen

@Composable
private fun RunScreen(config: RunConfig, onFinished: () -> Unit) {
    val context = LocalContext.current
    val resIds = remember(config) {
        val total = config.cellsPerRow * config.cellsPerRow
        List(total) { ASSET_RES_IDS[it % ASSET_RES_IDS.size] }
    }
    val ratios = remember(config) { resIds.map { aspectRatioOf(context, it) } }

    BackHandler { onFinished() }
    ImmersiveFullscreen()
    LaunchedEffect(config) {
        delay(WARMUP_MILLIS + config.durationMillis)
        onFinished()
    }

    when (config.host) {
        // View host: a pure ViewGroup so the LottieView render path is measured without Compose overhead.
        Host.View -> AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                LottieMultiViewGrid(ctx, config.cellsPerRow, config.sizeMode).apply {
                    setBackgroundColor(android.graphics.Color.BLACK)
                    populate(config.renderer, resIds, ratios)
                }
            }
        )
        Host.Compose -> Box(Modifier.fillMaxSize().background(Color.Black)) {
            LottieMultiComposeGrid(resIds, ratios, config.renderer.toComposeRenderer(), config.cellsPerRow, config.sizeMode)
        }
    }
}

/** Hides the system bars while the benchmark runs, restoring them on exit. */
@Composable
private fun ImmersiveFullscreen() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = view.context.activityWindow()
        if (window == null) {
            onDispose {}
        } else {
            val controller = WindowInsetsControllerCompat(window, view)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
        }
    }
}

// endregion

// region Helpers

private const val DEFAULT_CELLS_PER_ROW = 5
private const val MAX_CELLS_PER_ROW = 10
private val CELLS_PER_ROW_OPTIONS = (1..MAX_CELLS_PER_ROW).toList()
private const val DEFAULT_DURATION_SECONDS = 30
private const val DEFAULT_DURATION_MILLIS = DEFAULT_DURATION_SECONDS * 1000L
private const val MAX_DURATION_SECONDS = 60
private const val WARMUP_MILLIS = 2_000L

private val FORM_BACKGROUND = Color(0xFFF6F0E8)

// Curated assets; for a count beyond the set size they tile modularly (`resIds[i % size]`).
private val ASSET_RES_IDS = intArrayOf(
    R.raw.holdanimation,
    R.raw.page_slide,
    R.raw.puckerbloat,
    R.raw.starburst,
    R.raw.textblock,
    R.raw.emoji,
    R.raw.uk_flag,
    R.raw.lottie_32266,
    R.raw.shutup
)

private fun Renderer.label(): String = if (this == Renderer.Gl) "GL" else "SW"

private fun Renderer.toComposeRenderer(): LottieRenderer =
    if (this == Renderer.Gl) LottieRenderer.Gl else LottieRenderer.Sw

private fun String.toBoundedIntOr(default: Int, max: Int): Int =
    toIntOrNull()?.coerceIn(1, max) ?: default

private fun Context.activityWindow(): Window? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is Activity) return context.window
        context = context.baseContext
    }
    return null
}

private fun aspectRatioOf(context: Context, @RawRes resId: Int): Float = runCatching {
    val raw = context.resources.openRawResource(resId).use { it.bufferedReader().readText() }
    val json = JSONObject(raw)
    val w = json.optDouble("w", 1.0).toFloat()
    val h = json.optDouble("h", 1.0).toFloat()
    if (w > 0f && h > 0f) w / h else 1f
}.getOrDefault(1f)

// endregion
