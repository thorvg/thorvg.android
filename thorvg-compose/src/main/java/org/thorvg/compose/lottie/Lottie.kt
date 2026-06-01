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

package org.thorvg.compose.lottie

import android.graphics.Bitmap
import androidx.annotation.FloatRange
import androidx.annotation.RawRes
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Stable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.thorvg.core.lottie.LottieConstants
import org.thorvg.core.lottie.LottieSwComposition
import org.thorvg.core.lottie.LottieRenderState
import org.thorvg.core.lottie.LottieRepeatMode

/**
 * Remembers a mutable playback controller for [Lottie].
 */
@Composable
fun rememberLottieState(
    isPlaying: Boolean = true,
    repeatCount: Int = 0,
    @LottieRepeatMode repeatMode: Int = LottieConstants.RESTART,
    @FloatRange(from = 0.0) speed: Float = 1f
): LottieState {
    return remember {
        LottieState(
            isPlaying = isPlaying,
            repeatCount = repeatCount,
            repeatMode = repeatMode,
            speed = speed
        )
    }
}

/**
 * Mutable playback state used by the Compose ThorVG Lottie API.
 */
@Stable
class LottieState internal constructor(
    isPlaying: Boolean,
    repeatCount: Int,
    @LottieRepeatMode repeatMode: Int,
    speed: Float
) {
    /**
     * Whether the animation should currently advance frames.
     */
    var isPlaying by mutableStateOf(isPlaying)
        private set

    /**
     * Whether a render loop is currently active.
     */
    var isRunning by mutableStateOf(false)
        internal set

    /**
     * Current frame rendered by the Compose adapter.
     */
    var currentFrame by mutableIntStateOf(0)
        internal set

    /**
     * Number of times the animation should repeat after its first pass.
     */
    var repeatCount by mutableIntStateOf(repeatCount)

    /**
     * Repeat behavior used after reaching the end frame.
     */
    @get:LottieRepeatMode
    var repeatMode by mutableIntStateOf(repeatMode)

    /**
     * Playback speed multiplier.
     */
    var speed by mutableFloatStateOf(speed)

    internal var resetRequests by mutableIntStateOf(0)
        private set

    /**
     * Starts playback and resets the animation to its first frame.
     */
    fun play() {
        resetRequests++
        isPlaying = true
    }

    /**
     * Pauses playback without resetting the current frame.
     */
    fun pause() {
        isPlaying = false
    }

    /**
     * Resumes playback from the current frame.
     */
    fun resume() {
        isPlaying = true
    }

    /**
     * Stops playback and resets the animation to its first frame.
     */
    fun stop() {
        resetRequests++
        isPlaying = false
    }
}

/**
 * Remembers a [LottieSwComposition] loaded from a raw resource and releases it when it leaves composition.
 */
@Composable
fun rememberLottieComposition(@RawRes resId: Int): LottieSwComposition {
    val resources = LocalContext.current.resources
    val composition = remember(resources, resId) {
        LottieSwComposition.fromRawResource(resources, resId)
    }

    DisposableEffect(composition) {
        onDispose {
            composition.release()
        }
    }

    return composition
}

/**
 * Renders a ThorVG Lottie animation from a raw resource in Compose.
 *
 * Use [renderer] to pick between the default renderer ([LottieRenderer.Default]),
 * the software bitmap renderer ([LottieRenderer.Sw]), or the GPU renderer
 * ([LottieRenderer.Gl]). Any GL render failure falls back to SW rendering.
 */
@Composable
fun Lottie(
    @RawRes resId: Int,
    modifier: Modifier = Modifier,
    state: LottieState = rememberLottieState(),
    firstFrame: Int = 0,
    lastFrame: Int? = null,
    renderer: LottieRenderer = LottieRenderer.Default,
    onAnimationStart: (() -> Unit)? = null,
    onAnimationRepeat: (() -> Unit)? = null,
    onAnimationEnd: (() -> Unit)? = null
) {
    // Set to true when GL fails. Compose then switches to the SW path on the
    // next frame and cleans up the GL renderer and TextureView for us.
    var glRenderFailed by remember(renderer) { mutableStateOf(false) }

    val effective = when (renderer) {
        LottieRenderer.Default ->
            if (glRenderFailed) LottieRenderer.Sw else LottieRenderer.Gl
        LottieRenderer.Sw -> LottieRenderer.Sw
        LottieRenderer.Gl ->
            if (glRenderFailed) LottieRenderer.Sw else LottieRenderer.Gl
    }

    when (effective) {
        LottieRenderer.Sw -> {
            val composition = rememberLottieComposition(resId)
            Lottie(
                composition = composition,
                modifier = modifier,
                state = state,
                firstFrame = firstFrame,
                lastFrame = lastFrame,
                onAnimationStart = onAnimationStart,
                onAnimationRepeat = onAnimationRepeat,
                onAnimationEnd = onAnimationEnd
            )
        }

        LottieRenderer.Gl -> LottieGlAnimation(
            resId = resId,
            modifier = modifier,
            state = state,
            firstFrame = firstFrame,
            lastFrame = lastFrame,
            onAnimationStart = onAnimationStart,
            onAnimationRepeat = onAnimationRepeat,
            onAnimationEnd = onAnimationEnd,
            onRenderFailure = { glRenderFailed = true }
        )

        LottieRenderer.Default -> Unit
    }
}

/**
 * Renders a ThorVG [LottieSwComposition] in Compose.
 */
@Composable
fun Lottie(
    composition: LottieSwComposition,
    modifier: Modifier = Modifier,
    state: LottieState = rememberLottieState(),
    firstFrame: Int = 0,
    lastFrame: Int? = null,
    onAnimationStart: (() -> Unit)? = null,
    onAnimationRepeat: (() -> Unit)? = null,
    onAnimationEnd: (() -> Unit)? = null
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var currentBitmap by remember(composition) { mutableStateOf<Bitmap?>(null, neverEqualPolicy()) }
    var consumedResetRequest by remember(composition) { mutableIntStateOf(state.resetRequests) }

    LaunchedEffect(
        composition,
        canvasSize,
        state.isPlaying,
        state.repeatCount,
        state.repeatMode,
        state.speed,
        state.resetRequests,
        firstFrame,
        lastFrame
    ) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) {
            currentBitmap = null
            return@LaunchedEffect
        }

        val renderState = LottieRenderState().apply {
            this.composition = composition
            this.repeatMode = state.repeatMode
            this.repeatCount = state.repeatCount
            this.speed = state.speed
        }

        renderState.setFrameBounds(firstFrame, lastFrame)
        composition.setSize(canvasSize.width, canvasSize.height)

        var repeated = 0
        var started = false
        val shouldReset = state.resetRequests != consumedResetRequest
        consumedResetRequest = state.resetRequests

        val resolvedLastFrame = renderState.lastFrame
        var frame = if (shouldReset) {
            renderState.firstFrame
        } else {
            state.currentFrame.coerceIn(renderState.firstFrame, resolvedLastFrame)
        }

        state.isRunning = state.isPlaying

        while (isActive) {
            currentBitmap = composition.renderFrame(frame)
            state.currentFrame = frame

            if (!state.isPlaying) {
                break
            }

            if (!started) {
                started = true
                onAnimationStart?.invoke()
            }

            if (renderState.speed <= 0f) {
                break
            }

            val isFiniteEnd =
                renderState.repeatCount != LottieConstants.INFINITE &&
                    repeated == renderState.repeatCount &&
                    frame == resolvedLastFrame
            if (isFiniteEnd) {
                onAnimationEnd?.invoke()
                break
            }

            delay(renderState.frameInterval.coerceAtLeast(0L))

            var nextFrame = frame + renderState.framesPerUpdate
            var resetFrame = false
            if (nextFrame > resolvedLastFrame) {
                nextFrame = renderState.firstFrame
                resetFrame = true
            } else if (nextFrame < renderState.firstFrame) {
                nextFrame = resolvedLastFrame
                resetFrame = true
            }

            if (resetFrame) {
                repeated++
                onAnimationRepeat?.invoke()
            }

            frame = nextFrame
        }

        state.isRunning = false
    }

    Canvas(
        modifier = modifier.onSizeChanged { canvasSize = it }
    ) {
        currentBitmap?.let { bitmap ->
            drawImage(bitmap.asImageBitmap(), topLeft = Offset.Zero)
        }
    }
}
