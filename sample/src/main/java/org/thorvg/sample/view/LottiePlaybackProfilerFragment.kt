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

package org.thorvg.sample.view

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.Constraints
import androidx.fragment.app.Fragment
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.ceil
import kotlin.math.sqrt
import org.json.JSONObject
import org.thorvg.compose.lottie.Lottie
import org.thorvg.compose.lottie.LottieRenderer
import org.thorvg.compose.lottie.rememberLottieState
import org.thorvg.core.lottie.LottieConstants
import org.thorvg.sample.R
import org.thorvg.view.lottie.LottieView
import org.thorvg.view.lottie.Renderer
import org.thorvg.view.lottie.sw.LottieDrawable

class LottiePlaybackProfilerFragment : Fragment() {
    private val handler = Handler(Looper.getMainLooper())
    private val runningViews = mutableListOf<LottieView>()
    private val stopRunnable = Runnable { stopRun(showConfig = true) }
    private val aspectRatioCache = mutableMapOf<Int, Float>()

    private var rootView: FrameLayout? = null
    private var sizeMode = SizeMode.Fill
    private var toolbarWasVisible = true
    private var fullscreenActive = false
    // Last successfully started config; used to prefill the form when the
    // user returns to the configuration screen so they don't have to retype
    // values on every run within the same fragment session.
    private var lastConfig: RunConfig? = null

    private lateinit var backCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                stopRun(showConfig = true)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, backCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FrameLayout(requireContext()).also {
            rootView = it
            showConfig()
        }
    }

    override fun onDestroyView() {
        stopRun(showConfig = false)
        rootView = null
        super.onDestroyView()
    }

    private fun showConfig() {
        backCallback.isEnabled = false
        restoreSystemUi()

        val context = requireContext()
        val saved = lastConfig
        sizeMode = saved?.sizeMode ?: SizeMode.Fill

        val hostSpinner = context.spinner(listOf("View", "Compose")).apply {
            setSelection(if (saved?.host == Host.Compose) 1 else 0)
        }
        val rendererSpinner = context.spinner(listOf("Software", "GL")).apply {
            setSelection(if (saved?.renderer == Renderer.Gl) 1 else 0)
        }
        val assetSetSpinner = context.spinner(ASSET_SETS.map { it.name }).apply {
            val index = saved?.assetSet
                ?.let { ASSET_SETS.indexOf(it) }
                ?.takeIf { it >= 0 }
                ?: 0
            setSelection(index)
        }
        val countInput = context.numberInput((saved?.count ?: DEFAULT_COUNT).toString())
        val widthInput = context.numberInput((saved?.fixedWidth ?: DEFAULT_FIXED_SIZE).toString())
        val heightInput = context.numberInput((saved?.fixedHeight ?: DEFAULT_FIXED_SIZE).toString())
        val durationSeconds = saved?.durationMillis?.let { (it / 1000L).toInt() }
            ?: DEFAULT_DURATION_SECONDS
        val durationInput = context.numberInput(durationSeconds.toString())
        val sizeModeGroup = context.sizeModeGroup(
            initialMode = sizeMode,
            onModeChanged = {
                sizeMode = it
                val fixedEnabled = it == SizeMode.Fixed
                widthInput.isEnabled = fixedEnabled
                heightInput.isEnabled = fixedEnabled
            }
        )
        val fixedEnabled = sizeMode == SizeMode.Fixed
        widthInput.isEnabled = fixedEnabled
        heightInput.isEnabled = fixedEnabled
        val startButton = Button(context).apply {
            text = "Start"
            setOnClickListener {
                startRun(
                    RunConfig(
                        host = if (hostSpinner.selectedItemPosition == 1) Host.Compose else Host.View,
                        renderer = if (rendererSpinner.selectedItemPosition == 1) Renderer.Gl else Renderer.Sw,
                        assetSet = ASSET_SETS[assetSetSpinner.selectedItemPosition],
                        count = countInput.positiveIntOrDefault(DEFAULT_COUNT, max = MAX_COUNT),
                        sizeMode = sizeMode,
                        fixedWidth = widthInput.positiveIntOrDefault(DEFAULT_FIXED_SIZE, max = MAX_FIXED_SIZE),
                        fixedHeight = heightInput.positiveIntOrDefault(DEFAULT_FIXED_SIZE, max = MAX_FIXED_SIZE),
                        durationMillis = durationInput.positiveIntOrDefault(
                            DEFAULT_DURATION_SECONDS,
                            max = MAX_DURATION_SECONDS
                        ) * 1000L
                    )
                )
            }
        }

        val controls = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(20), context.dp(18), context.dp(20), context.dp(24))
            setBackgroundColor(Color.rgb(246, 240, 232))

            addHeader("Playback Profiler")
            addControl("Host", hostSpinner)
            addControl("Renderer", rendererSpinner)
            addControl("Asset set", assetSetSpinner)
            addControl("Count", countInput)
            addControl("Size", sizeModeGroup)
            addTwinControl("Width px", widthInput, "Height px", heightInput)
            addControl("Duration sec", durationInput)
            addView(
                startButton,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    context.dp(48)
                ).apply {
                    topMargin = context.dp(18)
                }
            )
        }

        rootView?.removeAllViews()
        rootView?.addView(
            ScrollView(context).apply {
                addView(
                    controls,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
            },
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun startRun(config: RunConfig) {
        lastConfig = config
        handler.removeCallbacks(stopRunnable)
        clearFocusedInput()
        releaseRunningViews()
        backCallback.isEnabled = true
        enterFullscreen()

        val resIds = (0 until config.count).map {
            config.assetSet.resIds[it % config.assetSet.resIds.size]
        }
        val ratios = resIds.map(::aspectRatioOf)

        val content: View = when (config.host) {
            Host.View -> buildViewGrid(config, resIds, ratios)
            Host.Compose -> buildComposeGrid(config, resIds, ratios)
        }

        rootView?.removeAllViews()
        rootView?.addView(
            content,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        if (config.host == Host.View) {
            content.post { runningViews.forEach { it.startAnimation() } }
        }
        handler.postDelayed(stopRunnable, WARMUP_MILLIS + config.durationMillis)
    }

    private fun buildViewGrid(
        config: RunConfig,
        resIds: List<Int>,
        ratios: List<Float>
    ): View {
        val context = requireContext()
        val grid = ProfilerGridLayout(context).apply {
            setBackgroundColor(Color.BLACK)
            setConfig(config.count, config.sizeMode, config.fixedWidth, config.fixedHeight)
        }
        resIds.forEachIndexed { index, resId ->
            val view = LottieView(context).apply {
                setRenderer(config.renderer)
                setRawRes(resId)
                setRepeatCount(LottieDrawable.INFINITE)
                setAutoStart(false)
                // Stash the lottie's intrinsic aspect ratio so the grid can
                // letterbox each cell instead of stretching different shapes
                // (1:1, 9:16, 16:9, ...) to the same rectangle.
                tag = ratios[index]
            }
            runningViews += view
            grid.addView(view)
        }
        return grid
    }

    private fun buildComposeGrid(
        config: RunConfig,
        resIds: List<Int>,
        ratios: List<Float>
    ): View {
        val composeRenderer = if (config.renderer == Renderer.Gl) {
            LottieRenderer.Gl
        } else {
            LottieRenderer.Sw
        }
        return ComposeView(requireContext()).apply {
            setBackgroundColor(Color.BLACK)
            setContent {
                ProfilerComposeGrid(
                    resIds = resIds,
                    aspectRatios = ratios,
                    renderer = composeRenderer,
                    sizeMode = config.sizeMode,
                    fixedWidth = config.fixedWidth,
                    fixedHeight = config.fixedHeight
                )
            }
        }
    }

    private fun stopRun(showConfig: Boolean) {
        handler.removeCallbacks(stopRunnable)
        releaseRunningViews()
        if (showConfig && rootView != null) {
            showConfig()
        } else {
            restoreSystemUi()
        }
    }

    private fun releaseRunningViews() {
        runningViews.forEach {
            it.stopAnimation()
        }
        runningViews.clear()
    }

    private fun enterFullscreen() {
        val activity = requireActivity() as AppCompatActivity
        val toolbar = activity.findViewById<View>(R.id.toolbar)
        toolbarWasVisible = toolbar?.visibility == View.VISIBLE
        toolbar?.visibility = View.GONE

        activity.supportActionBar?.hide()
        hideSystemBars(activity.window)
        fullscreenActive = true
    }

    private fun restoreSystemUi() {
        if (!fullscreenActive) return

        val activity = requireActivity() as AppCompatActivity
        val toolbar = activity.findViewById<View>(R.id.toolbar)
        if (toolbarWasVisible) toolbar?.visibility = View.VISIBLE

        activity.supportActionBar?.show()
        showSystemBars(activity.window)
        fullscreenActive = false
    }

    private fun hideSystemBars(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun showSystemBars(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).show(
            WindowInsetsCompat.Type.systemBars()
        )
    }

    private fun Context.spinner(items: List<String>): Spinner {
        return Spinner(this).apply {
            adapter = ArrayAdapter(
                this@spinner,
                android.R.layout.simple_spinner_item,
                items
            ).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }
    }

    private fun Context.numberInput(value: String): EditText {
        return EditText(this).apply {
            setText(value)
            setSelectAllOnFocus(true)
            inputType = InputType.TYPE_CLASS_NUMBER
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }
    }

    private fun clearFocusedInput() {
        val focused = requireActivity().currentFocus
        focused?.clearFocus()
        val token = focused?.windowToken ?: rootView?.windowToken ?: return
        val inputMethodManager = requireContext().getSystemService(
            Context.INPUT_METHOD_SERVICE
        ) as? InputMethodManager
        inputMethodManager?.hideSoftInputFromWindow(token, 0)
    }

    private fun Context.sizeModeGroup(
        initialMode: SizeMode,
        onModeChanged: (SizeMode) -> Unit
    ): RadioGroup {
        return RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            val fillButton = RadioButton(context).apply {
                id = View.generateViewId()
                text = "FILL"
                isChecked = initialMode == SizeMode.Fill
            }
            val fixedButton = RadioButton(context).apply {
                id = View.generateViewId()
                text = "Fixed"
                isChecked = initialMode == SizeMode.Fixed
            }
            addView(fillButton)
            addView(fixedButton)
            setOnCheckedChangeListener { _, checkedId ->
                onModeChanged(if (checkedId == fixedButton.id) SizeMode.Fixed else SizeMode.Fill)
            }
        }
    }

    private fun LinearLayout.addHeader(title: String) {
        addView(
            TextView(context).apply {
                text = title
                textSize = 22f
                setTextColor(Color.rgb(33, 35, 42))
                setPadding(0, 0, 0, context.dp(12))
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun LinearLayout.addControl(label: String, control: View) {
        addView(
            TextView(context).apply {
                text = label
                textSize = 13f
                setTextColor(Color.rgb(91, 82, 71))
                setPadding(0, context.dp(12), 0, context.dp(4))
            }
        )
        addView(
            control,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun LinearLayout.addTwinControl(
        firstLabel: String,
        firstControl: View,
        secondLabel: String,
        secondControl: View
    ) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    addControl(firstLabel, firstControl)
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    addControl(secondLabel, secondControl)
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = context.dp(12)
                }
            )
        }
        addView(row)
    }

    private fun EditText.positiveIntOrDefault(defaultValue: Int, max: Int): Int {
        return text
            ?.toString()
            ?.toIntOrNull()
            ?.coerceIn(1, max)
            ?: defaultValue
    }

    private fun Context.dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun aspectRatioOf(@RawRes resId: Int): Float {
        aspectRatioCache[resId]?.let { return it }

        val ratio = try {
            val raw = requireContext().resources.openRawResource(resId).use {
                it.bufferedReader().readText()
            }
            val json = JSONObject(raw)
            val w = json.optDouble("w", 1.0).toFloat()
            val h = json.optDouble("h", 1.0).toFloat()
            if (w > 0f && h > 0f) w / h else 1f
        } catch (e: Exception) {
            1f
        }
        aspectRatioCache[resId] = ratio
        return ratio
    }

    private class ProfilerGridLayout(context: Context) : ViewGroup(context) {
        private var activeCount = 0
        private var sizeMode = SizeMode.Fill
        private var fixedWidth = DEFAULT_FIXED_SIZE
        private var fixedHeight = DEFAULT_FIXED_SIZE

        fun setConfig(count: Int, mode: SizeMode, width: Int, height: Int) {
            activeCount = count
            sizeMode = mode
            fixedWidth = width
            fixedHeight = height
            requestLayout()
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val height = MeasureSpec.getSize(heightMeasureSpec)
            if (childCount == 0 || width <= 0 || height <= 0) {
                setMeasuredDimension(width, height)
                return
            }

            val rows = gridRows(activeChildCount())
            var childIndex = 0
            for (row in 0 until rows) {
                val itemsInRow = gridItemsInRow(activeChildCount(), rows, row)
                val cellHeight = gridSpan(height, rows, row)
                repeat(itemsInRow) {
                    val cellWidth = gridSpan(width, itemsInRow, it)
                    val child = getChildAt(childIndex)
                    val ratio = (child.tag as? Float)?.takeIf { r -> r > 0f } ?: 1f
                    val (childW, childH) = fitInside(
                        if (sizeMode == SizeMode.Fill) cellWidth else fixedWidth,
                        if (sizeMode == SizeMode.Fill) cellHeight else fixedHeight,
                        ratio
                    )
                    child.measure(
                        MeasureSpec.makeMeasureSpec(childW.coerceAtLeast(1), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(childH.coerceAtLeast(1), MeasureSpec.EXACTLY)
                    )
                    childIndex++
                }
            }
            setMeasuredDimension(width, height)
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            val width = measuredWidth
            val height = measuredHeight
            if (childCount == 0 || width <= 0 || height <= 0) return

            val rows = gridRows(activeChildCount())
            var childIndex = 0
            var childTop = 0
            for (row in 0 until rows) {
                val itemsInRow = gridItemsInRow(activeChildCount(), rows, row)
                val childBottom = gridEdge(height, rows, row)
                var childLeft = 0
                for (column in 0 until itemsInRow) {
                    val childRight = gridEdge(width, itemsInRow, column)
                    val child = getChildAt(childIndex)
                    val availableWidth = childRight - childLeft
                    val availableHeight = childBottom - childTop
                    val childStart = childLeft + (availableWidth - child.measuredWidth) / 2
                    val childTopAligned = childTop + (availableHeight - child.measuredHeight) / 2
                    child.layout(
                        childStart,
                        childTopAligned,
                        childStart + child.measuredWidth,
                        childTopAligned + child.measuredHeight
                    )
                    childLeft = childRight
                    childIndex++
                }
                childTop = childBottom
            }
        }

        private fun activeChildCount(): Int {
            return activeCount.takeIf { it > 0 } ?: childCount
        }
    }

    private enum class SizeMode {
        Fill,
        Fixed
    }

    /** Mirrors [ProfilerGridLayout] for the Compose host so View vs Compose comparisons share layout. */
    @Composable
    private fun ProfilerComposeGrid(
        resIds: List<Int>,
        aspectRatios: List<Float>,
        renderer: LottieRenderer,
        sizeMode: SizeMode,
        fixedWidth: Int,
        fixedHeight: Int
    ) {
        Layout(
            modifier = Modifier.fillMaxSize(),
            content = { ProfilerComposeItems(resIds, renderer) }
        ) { measurables, constraints ->
            measureProfilerGrid(measurables, constraints, aspectRatios, sizeMode, fixedWidth, fixedHeight)
        }
    }

    @Composable
    private fun ProfilerComposeItems(
        resIds: List<Int>,
        renderer: LottieRenderer
    ) {
        resIds.forEach { resId ->
            val state = rememberLottieState(
                isPlaying = true,
                repeatCount = LottieConstants.INFINITE
            )
            Lottie(
                resId = resId,
                state = state,
                renderer = renderer
            )
        }
    }

    private fun MeasureScope.measureProfilerGrid(
        measurables: List<Measurable>,
        constraints: Constraints,
        aspectRatios: List<Float>,
        sizeMode: SizeMode,
        fixedWidth: Int,
        fixedHeight: Int
    ): MeasureResult {
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val count = measurables.size
        if (count == 0 || width <= 0 || height <= 0) {
            return layout(width, height) {}
        }

        val cells = composeGridCells(count, width, height)
        val placed = measureComposeGridChildren(
            measurables = measurables,
            cells = cells,
            aspectRatios = aspectRatios,
            sizeMode = sizeMode,
            fixedWidth = fixedWidth,
            fixedHeight = fixedHeight
        )
        return layout(width, height) {
            placed.forEach { it.placeable.place(it.x, it.y) }
        }
    }

    private fun composeGridCells(count: Int, width: Int, height: Int): List<ComposeGridCell> {
        val rows = gridRows(count)
        val cells = ArrayList<ComposeGridCell>(count)

        var childTop = 0
        for (row in 0 until rows) {
            val itemsInRow = gridItemsInRow(count, rows, row)
            val childBottom = gridEdge(height, rows, row)
            var childLeft = 0
            for (column in 0 until itemsInRow) {
                val childRight = gridEdge(width, itemsInRow, column)
                cells += ComposeGridCell(
                    left = childLeft,
                    top = childTop,
                    right = childRight,
                    bottom = childBottom
                )
                childLeft = childRight
            }
            childTop = childBottom
        }

        return cells
    }

    private fun measureComposeGridChildren(
        measurables: List<Measurable>,
        cells: List<ComposeGridCell>,
        aspectRatios: List<Float>,
        sizeMode: SizeMode,
        fixedWidth: Int,
        fixedHeight: Int
    ): List<PlacedComposeChild> {
        return measurables.mapIndexed { index, measurable ->
            val cell = cells[index]
            val ratio = aspectRatios.getOrElse(index) { 1f }.takeIf { it > 0f } ?: 1f
            val (childW, childH) = fitInside(
                if (sizeMode == SizeMode.Fill) cell.width else fixedWidth,
                if (sizeMode == SizeMode.Fill) cell.height else fixedHeight,
                ratio
            )
            val placeable = measurable.measure(
                Constraints.fixed(childW.coerceAtLeast(1), childH.coerceAtLeast(1))
            )
            PlacedComposeChild(
                placeable = placeable,
                x = cell.left + (cell.width - childW) / 2,
                y = cell.top + (cell.height - childH) / 2
            )
        }
    }

    private data class ComposeGridCell(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    ) {
        val width: Int
            get() = right - left
        val height: Int
            get() = bottom - top
    }

    private data class PlacedComposeChild(
        val placeable: Placeable,
        val x: Int,
        val y: Int
    )

    private data class RunConfig(
        val host: Host,
        val renderer: Renderer,
        val assetSet: AssetSet,
        val count: Int,
        val sizeMode: SizeMode,
        val fixedWidth: Int,
        val fixedHeight: Int,
        val durationMillis: Long
    )

    private enum class Host {
        View,
        Compose
    }

    private data class AssetSet(
        val name: String,
        @RawRes val resIds: IntArray
    ) {
        override fun equals(other: Any?): Boolean {
            return other is AssetSet && name == other.name && resIds.contentEquals(other.resIds)
        }

        override fun hashCode(): Int {
            return 31 * name.hashCode() + resIds.contentHashCode()
        }
    }

    private companion object {
        private const val DEFAULT_COUNT = 25
        private const val DEFAULT_FIXED_SIZE = 200
        private const val DEFAULT_DURATION_SECONDS = 30
        private const val MAX_COUNT = 200
        private const val MAX_FIXED_SIZE = 4096
        private const val MAX_DURATION_SECONDS = 3600
        private const val WARMUP_MILLIS = 2_000L

        // Curated 10-asset set; if Count exceeds 10 the same assets are tiled
        // modularly (`resIds[i % size]`) so any Count is still well-defined.
        private val ASSET_SETS = listOf(
            AssetSet(
                name = "Lottie top 10 file size",
                resIds = intArrayOf(
                    R.raw.holdanimation,
                    R.raw.page_slide,
                    R.raw.puckerbloat,
                    R.raw.starburst,
                    R.raw.textblock,
                    // R.raw.ghost,
                    R.raw.emoji,
                    R.raw.uk_flag,
                    R.raw.lottie_32266,
                    R.raw.shutup
                )
            )
        )
    }
}

// Shared square-ish grid math used by both the View and Compose hosts.

private fun gridRows(count: Int): Int = ceil(sqrt(count.toDouble())).toInt().coerceAtLeast(1)

private fun gridItemsInRow(count: Int, rows: Int, row: Int): Int =
    count / rows + if (row < count % rows) 1 else 0

/** Cumulative pixel edge of partition [index] when [total] is split into [parts] (telescopes to exactly [total]). */
private fun gridEdge(total: Int, parts: Int, index: Int): Int = total * (index + 1) / parts

/** Pixel size of partition [index] (gridEdge(index) − gridEdge(index − 1)). */
private fun gridSpan(total: Int, parts: Int, index: Int): Int = gridEdge(total, parts, index) - total * index / parts

/** Largest [ratio]-preserving (w to h) size that fits within [maxW] × [maxH] (letterbox). */
private fun fitInside(maxW: Int, maxH: Int, ratio: Float): Pair<Int, Int> {
    val byWidth = (maxW / ratio).toInt()
    return if (byWidth <= maxH) maxW to byWidth else (maxH * ratio).toInt() to maxH
}
