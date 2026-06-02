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

import org.thorvg.view.lottie.Renderer

// region Run configuration

/** A single profiling run, shared by the config form and the run screen. */
internal data class RunConfig(
    val host: Host,
    val renderer: Renderer,
    /** Cells per row; the grid is laid out as [cellsPerRow] × [cellsPerRow]. */
    val cellsPerRow: Int,
    val sizeMode: SizeMode,
    val durationMillis: Long
)

/** Which rendering host drives the grid: classic Views or Compose. */
internal enum class Host { View, Compose }

/** How each grid cell sizes its lottie. */
internal enum class SizeMode {
    /** Fit inside each cell, preserving the lottie's aspect ratio (letterbox). */
    Fit {
        override fun size(cellWidth: Int, cellHeight: Int, ratio: Float) = fitInside(cellWidth, cellHeight, ratio)
    },
    /** Stretch to the full cell, ignoring aspect ratio. */
    Stretch {
        override fun size(cellWidth: Int, cellHeight: Int, ratio: Float) = cellWidth to cellHeight
    };

    abstract fun size(cellWidth: Int, cellHeight: Int, ratio: Float): Pair<Int, Int>
}

/** Largest [ratio]-preserving (w to h) size that fits within [maxW] × [maxH] (letterbox). */
private fun fitInside(maxW: Int, maxH: Int, ratio: Float): Pair<Int, Int> {
    val byWidth = (maxW / ratio).toInt()
    return if (byWidth <= maxH) maxW to byWidth else (maxH * ratio).toInt() to maxH
}

// endregion

// region Grid layout (shared by the View and Compose hosts)

internal data class GridCell(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width get() = right - left
    val height get() = bottom - top
}

/**
 * Iterates the cells of a [cellsPerRow] × [cellsPerRow] grid filling [width] × [height], row-major.
 * Edges are `size * i / cellsPerRow`, so adjacent cells share boundaries exactly (no rounding gaps).
 */
internal fun forEachGridCell(cellsPerRow: Int, width: Int, height: Int, action: (index: Int, cell: GridCell) -> Unit) {
    var index = 0
    for (row in 0 until cellsPerRow) {
        val top = height * row / cellsPerRow
        val bottom = height * (row + 1) / cellsPerRow
        for (column in 0 until cellsPerRow) {
            val left = width * column / cellsPerRow
            val right = width * (column + 1) / cellsPerRow
            action(index++, GridCell(left, top, right, bottom))
        }
    }
}

// endregion
