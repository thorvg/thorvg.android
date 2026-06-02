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

import android.content.Context
import android.view.View
import android.view.ViewGroup
import org.thorvg.view.lottie.LottieView
import org.thorvg.view.lottie.Renderer
import org.thorvg.view.lottie.sw.LottieDrawable

/** View host grid. Children carry their aspect ratio in [View.tag] and play while attached. */
internal class LottieMultiViewGrid(
    context: Context,
    private val cellsPerRow: Int,
    private val sizeMode: SizeMode
) : ViewGroup(context) {

    fun populate(renderer: Renderer, resIds: List<Int>, ratios: List<Float>) {
        removeAllViews()
        resIds.forEachIndexed { index, resId ->
            addView(LottieView(context).apply {
                setRenderer(renderer)
                setRawRes(resId)
                setRepeatCount(LottieDrawable.INFINITE)
                tag = ratios[index]
            })
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post { forEachLottie { it.startAnimation() } }
    }

    override fun onDetachedFromWindow() {
        forEachLottie { it.stopAnimation() }
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        forEachGridCell(cellsPerRow, width, height) { index, cell ->
            val child = getChildAt(index)
            val (childW, childH) = sizeMode.size(cell.width, cell.height, child.ratio)
            child.measure(
                MeasureSpec.makeMeasureSpec(childW, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(childH, MeasureSpec.EXACTLY)
            )
        }
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        forEachGridCell(cellsPerRow, measuredWidth, measuredHeight) { index, cell ->
            val child = getChildAt(index)
            val x = cell.left + (cell.width - child.measuredWidth) / 2
            val y = cell.top + (cell.height - child.measuredHeight) / 2
            child.layout(x, y, x + child.measuredWidth, y + child.measuredHeight)
        }
    }

    private inline fun forEachLottie(action: (LottieView) -> Unit) {
        for (i in 0 until childCount) (getChildAt(i) as? LottieView)?.let(action)
    }

    private val View.ratio: Float get() = (tag as? Float)?.takeIf { it > 0f } ?: 1f
}
