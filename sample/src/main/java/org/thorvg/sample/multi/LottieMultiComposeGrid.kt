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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import org.thorvg.compose.lottie.Lottie
import org.thorvg.compose.lottie.LottieRenderer
import org.thorvg.compose.lottie.rememberLottieState
import org.thorvg.core.lottie.LottieConstants

/** Compose host grid, mirroring [LottieMultiViewGrid] so View vs Compose comparisons share layout. */
@Composable
internal fun LottieMultiComposeGrid(
    resIds: List<Int>,
    aspectRatios: List<Float>,
    renderer: LottieRenderer,
    cellsPerRow: Int,
    sizeMode: SizeMode
) {
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            resIds.forEach { resId ->
                val state = rememberLottieState(isPlaying = true, repeatCount = LottieConstants.INFINITE)
                Lottie(resId = resId, state = state, renderer = renderer)
            }
        }
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val cells = buildList { forEachGridCell(cellsPerRow, width, height) { _, cell -> add(cell) } }
        val placed = measurables.mapIndexed { index, measurable ->
            val cell = cells[index]
            val ratio = aspectRatios.getOrElse(index) { 1f }.takeIf { it > 0f } ?: 1f
            val (childW, childH) = sizeMode.size(cell.width, cell.height, ratio)
            PlacedChild(
                placeable = measurable.measure(Constraints.fixed(childW, childH)),
                x = cell.left + (cell.width - childW) / 2,
                y = cell.top + (cell.height - childH) / 2
            )
        }
        layout(width, height) { placed.forEach { it.placeable.place(it.x, it.y) } }
    }
}

private data class PlacedChild(val placeable: Placeable, val x: Int, val y: Int)
