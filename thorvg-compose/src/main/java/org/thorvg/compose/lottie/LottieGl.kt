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

package org.thorvg.compose.lottie

import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.isActive
import org.thorvg.core.lottie.LottieGlComposition
import org.thorvg.core.lottie.LottieRenderState
import org.thorvg.core.lottie.gl.GlRenderer

/**
 * GL-backed Lottie composable. Hosts a [TextureView] driven by a shared GL thread.
 *
 * Selected by [Lottie] when [LottieRenderer.Gl] is requested; not intended for
 * direct use.
 */
@Composable
internal fun LottieGlAnimation(
    @RawRes resId: Int,
    modifier: Modifier,
    state: LottieState,
    firstFrame: Int,
    lastFrame: Int?,
    onAnimationStart: (() -> Unit)?,
    onAnimationRepeat: (() -> Unit)?,
    onAnimationEnd: (() -> Unit)?,
    onRenderFailure: (() -> Unit)? = null
) {
    val resources = LocalContext.current.resources
    val currentOnAnimationStart by rememberUpdatedState(onAnimationStart)
    val currentOnAnimationEnd by rememberUpdatedState(onAnimationEnd)
    val currentOnAnimationRepeat by rememberUpdatedState(onAnimationRepeat)
    val currentOnRenderFailure by rememberUpdatedState(onRenderFailure)

    val renderer = remember {
        GlRenderer(
            onAnimationStart = { currentOnAnimationStart?.invoke() },
            onAnimationEnd = { currentOnAnimationEnd?.invoke() },
            onAnimationRepeat = { currentOnAnimationRepeat?.invoke() },
            onRenderFailure = { currentOnRenderFailure?.invoke() }
        )
    }

    DisposableEffect(renderer) {
        onDispose { renderer.release() }
    }

    LaunchedEffect(
        renderer,
        resId,
        firstFrame,
        lastFrame,
        state.repeatCount,
        state.repeatMode,
        state.speed
    ) {
        val factory: (() -> LottieGlComposition?)? = if (resId != 0) {
            { LottieGlComposition.fromRawResource(resources, resId) }
        } else {
            null
        }
        val renderState = LottieRenderState().apply {
            this.repeatCount = state.repeatCount
            this.repeatMode = state.repeatMode
            this.speed = state.speed
            this.autoPlay = state.isPlaying
        }
        renderer.setCompositionFactory(factory, renderState, firstFrame, lastFrame)
    }

    var consumedResetRequest by remember(renderer) { mutableIntStateOf(state.resetRequests) }

    LaunchedEffect(renderer, state.isPlaying, state.resetRequests) {
        val didReset = state.resetRequests != consumedResetRequest
        consumedResetRequest = state.resetRequests
        when {
            state.isPlaying && didReset -> renderer.start()
            state.isPlaying -> renderer.resume()
            didReset -> renderer.stop()
            else -> renderer.pause()
        }
    }

    LaunchedEffect(renderer) {
        while (isActive) {
            withFrameNanos { }
            state.currentFrame = renderer.currentFrame
            state.isRunning = renderer.isRunning
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextureView(context).apply {
                isOpaque = false
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {
                        surface.setDefaultBufferSize(width, height)
                        renderer.setSurface(surface, width, height)
                    }

                    override fun onSurfaceTextureSizeChanged(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {
                        surface.setDefaultBufferSize(width, height)
                        renderer.resize(width, height)
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        renderer.clearSurface()
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
                }
            }
        }
    )
}
