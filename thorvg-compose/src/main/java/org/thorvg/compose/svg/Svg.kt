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

package org.thorvg.compose.svg

import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.RawRes
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import org.thorvg.core.svg.SvgComposition

/**
 * Remembers a [SvgComposition] loaded from a raw resource and releases it when it leaves composition.
 */
@Composable
fun rememberSvgComposition(@RawRes resId: Int): SvgComposition {
    val resources = LocalContext.current.resources
    val composition = remember(resources, resId) {
        SvgComposition.fromRawResource(resources, resId)
    }

    DisposableEffect(composition) {
        onDispose {
            composition.release()
        }
    }

    return composition
}

/**
 * Remembers a [SvgComposition] loaded from an asset file and releases it when it leaves composition.
 */
@Composable
fun rememberSvgComposition(assetName: String): SvgComposition {
    val assetManager = LocalContext.current.assets
    val composition = remember(assetManager, assetName) {
        SvgComposition.fromAsset(assetManager, assetName)
    }

    DisposableEffect(composition) {
        onDispose {
            composition.release()
        }
    }

    return composition
}

/**
 * Remembers a [SvgComposition] loaded from a [Uri] and releases it when it leaves composition.
 */
@Composable
fun rememberSvgComposition(uri: Uri): SvgComposition {
    val context = LocalContext.current
    val composition = remember(context, uri) {
        SvgComposition.fromUri(context, uri)
    }

    DisposableEffect(composition) {
        onDispose {
            composition.release()
        }
    }

    return composition
}

/**
 * Renders a ThorVG SVG from a raw resource in Compose.
 */
@Composable
fun Svg(
    @RawRes resId: Int,
    modifier: Modifier = Modifier
) {
    val composition = rememberSvgComposition(resId)
    Svg(
        composition = composition,
        modifier = modifier
    )
}

/**
 * Renders a ThorVG SVG from an asset file in Compose.
 */
@Composable
fun Svg(
    assetName: String,
    modifier: Modifier = Modifier
) {
    val composition = rememberSvgComposition(assetName)
    Svg(
        composition = composition,
        modifier = modifier
    )
}

/**
 * Renders a ThorVG SVG from a [Uri] in Compose.
 */
@Composable
fun Svg(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    val composition = rememberSvgComposition(uri)
    Svg(
        composition = composition,
        modifier = modifier
    )
}

/**
 * Renders a ThorVG [SvgComposition] in Compose.
 */
@Composable
fun Svg(
    composition: SvgComposition,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var currentBitmap by remember(composition) { mutableStateOf<Bitmap?>(null) }
    var renderedSize by remember(composition) { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(composition, canvasSize) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) {
            currentBitmap = null
            renderedSize = IntSize.Zero
            return@LaunchedEffect
        }

        val intrinsicWidth = composition.width
        val intrinsicHeight = composition.height
        val targetSize = if (intrinsicWidth > 0 && intrinsicHeight > 0) {
            val scale = minOf(
                canvasSize.width.toFloat() / intrinsicWidth,
                canvasSize.height.toFloat() / intrinsicHeight
            )
            IntSize(
                width = (intrinsicWidth * scale).toInt().coerceAtLeast(1),
                height = (intrinsicHeight * scale).toInt().coerceAtLeast(1)
            )
        } else {
            canvasSize
        }

        composition.setSize(targetSize.width, targetSize.height)
        currentBitmap = composition.render()
        renderedSize = targetSize
    }

    Canvas(
        modifier = modifier.onSizeChanged { canvasSize = it }
    ) {
        currentBitmap?.let { bitmap ->
            val topLeft = Offset(
                x = ((size.width - renderedSize.width) / 2f).coerceAtLeast(0f),
                y = ((size.height - renderedSize.height) / 2f).coerceAtLeast(0f)
            )
            drawImage(bitmap.asImageBitmap(), topLeft = topLeft)
        }
    }
}
