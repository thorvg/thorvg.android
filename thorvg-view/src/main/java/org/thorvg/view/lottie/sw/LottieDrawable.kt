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

package org.thorvg.view.lottie.sw

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import androidx.annotation.RawRes
import androidx.annotation.FloatRange
import org.thorvg.core.lottie.LottieConstants
import org.thorvg.core.lottie.LottieSwComposition
import org.thorvg.core.lottie.LottieRenderState
import org.thorvg.core.lottie.LottieRepeatMode
import org.thorvg.view.ThorVGDrawable
import org.thorvg.view.lottie.LottieListener

/**
 * Drawable adapter that renders a ThorVG Lottie composition into an Android [Canvas].
 */
class LottieDrawable internal constructor() : ThorVGDrawable(), Animatable {
    private var lottieState: LottieDrawableState = LottieDrawableState()
    private var listener: LottieListener? = null

    private var isRunning = false
    private var isEnded = false
    private var isStarted = false
    private var repeated = 0
    private var frame = 0

    private val handler = Handler(Looper.getMainLooper())
    private val nextFrameRunnable = Runnable { invalidateSelf() }
    private val tmpPaint = Paint()

    private var mutated = false

    internal constructor(state: LottieDrawableState) : this() {
        lottieState = state
    }

    /**
     * Releases the underlying composition and bitmap buffer.
     *
     * Call this when the drawable is no longer needed.
     */
    override fun release() {
        lottieState.releaseComposition()
    }

    override fun mutate(): Drawable {
        if (!mutated && super.mutate() === this) {
            lottieState = LottieDrawableState(lottieState)
            mutated = true
        }
        return this
    }

    override fun draw(canvas: Canvas) {
        if (lottieState.valid() && isRunning) {
            if (!isStarted) {
                isStarted = true
                dispatchAnimationStart()
            }

            val startTime = System.nanoTime()

            getFrame(frame)?.let { bitmap ->
                canvas.drawBitmap(bitmap, 0f, 0f, tmpPaint)
            }

            if (lottieState.repeatCount != INFINITE && repeated == lottieState.repeatCount) {
                if (!isEnded) {
                    isEnded = true
                    dispatchAnimationEnd()
                }
            } else {
                val lastFrame = lottieState.lastFrame
                var resetFrame = false
                frame += lottieState.framesPerUpdate
                if (frame > lastFrame) {
                    frame = lottieState.firstFrame
                    resetFrame = true
                } else if (frame < lottieState.firstFrame) {
                    frame = lastFrame
                    resetFrame = true
                }
                if (resetFrame) {
                    repeated++
                    dispatchAnimationRepeat()
                }
            }

            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
            handler.postDelayed(
                nextFrameRunnable,
                (lottieState.frameInterval - elapsedMs).coerceAtLeast(0L)
            )
        }
    }

    /**
     * Returns the bitmap containing the requested frame.
     *
     * The returned bitmap is owned by this drawable and may be reused on the next frame render.
     */
    fun getFrame(frame: Int): Bitmap? {
        return lottieState.renderFrame(frame)
    }

    override fun setAlpha(alpha: Int) = Unit

    @Deprecated("Deprecated in Drawable")
    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Deprecated in Drawable")
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun getIntrinsicWidth(): Int {
        return lottieState.width
    }

    override fun getIntrinsicHeight(): Int {
        return lottieState.height
    }

    /**
     * Number of times the animation repeats after its first pass.
     *
     * Use [INFINITE] to loop forever.
     */
    var repeatCount: Int
        get() = lottieState.repeatCount
        set(count) {
            lottieState.repeatCount = count
            repeated = 0
        }

    /**
     * Sets the repeat behavior to either [RESTART] or [REVERSE].
     */
    fun setRepeatMode(@LottieRepeatMode mode: Int) {
        lottieState.repeatMode = mode
    }

    /**
     * Current repeat behavior.
     */
    @get:LottieRepeatMode
    val repeatMode: Int
        get() = lottieState.repeatMode

    /**
     * First frame used for playback.
     */
    val firstFrame: Int
        get() = lottieState.firstFrame

    /**
     * Sets the frame range that playback is allowed to render.
     */
    fun setFrameBounds(firstFrame: Int, lastFrame: Int? = null) {
        lottieState.setFrameBounds(firstFrame, lastFrame)
    }

    /**
     * Last frame used for playback.
     */
    val lastFrame: Int
        get() = lottieState.lastFrame

    /**
     * Whether playback should start automatically after the drawable is attached by its host.
     */
    val isAutoPlay: Boolean
        get() = lottieState.autoPlay

    /**
     * Total animation duration in milliseconds.
     */
    val duration: Long
        get() = if (lottieState.valid()) lottieState.composition?.duration ?: 0L else 0L

    /**
     * Playback speed multiplier.
     *
     * Values larger than `1f` play faster. `0f` pauses frame progression.
     */
    var speed: Float
        @FloatRange(from = 0.0)
        get() = lottieState.speed
        set(@FloatRange(from = 0.0) value) {
            lottieState.speed = value
        }

    /**
     * Resizes the composition buffer to the given dimensions in pixels.
     */
    override fun setSize(width: Int, height: Int) {
        require(width > 0) { "LottieDrawable requires width > 0" }
        require(height > 0) { "LottieDrawable requires height > 0" }
        lottieState.setCompositionSize(width, height)
    }

    override fun isRunning(): Boolean {
        return isRunning
    }

    /**
     * Starts playback from [firstFrame].
     */
    override fun start() {
        handler.removeCallbacks(nextFrameRunnable)
        isRunning = true
        isEnded = false
        isStarted = false
        repeated = 0
        frame = lottieState.firstFrame
        invalidateSelf()
    }

    /**
     * Stops playback and removes any scheduled invalidation callbacks.
     */
    override fun stop() {
        isRunning = false
        handler.removeCallbacks(nextFrameRunnable)
    }

    /**
     * Pauses playback without resetting the current frame.
     */
    fun pause() {
        isRunning = false
        handler.removeCallbacks(nextFrameRunnable)
    }

    /**
     * Resumes playback from the current frame.
     */
    fun resume() {
        isRunning = true
        invalidateSelf()
    }

    /**
     * Current frame index being rendered by this drawable.
     */
    val currentFrame: Int
        get() = frame

    /**
     * Registers a listener for playback lifecycle callbacks.
     */
    fun setAnimationListener(listener: LottieListener?) {
        this.listener = listener
    }

    internal fun dispatchAnimationStart() {
        listener?.onAnimationStart()
    }

    internal fun dispatchAnimationRepeat() {
        listener?.onAnimationRepeat()
    }

    internal fun dispatchAnimationEnd() {
        listener?.onAnimationEnd()
    }

    internal class LottieDrawableState() : ConstantState() {
        private val renderState = LottieRenderState()

        var composition: LottieSwComposition? = null
            get() = field
            set(value) {
                field = value
                renderState.composition = value
                if (value != null && width > 0 && height > 0) {
                    value.setSize(width, height)
                }
            }

        var width = 0

        var height = 0

        var repeatCount: Int
            get() = renderState.repeatCount
            set(value) {
                renderState.repeatCount = value
            }

        var repeatMode: Int
            get() = renderState.repeatMode
            set(value) {
                renderState.repeatMode = value
            }

        var framesPerUpdate: Int
            get() = renderState.framesPerUpdate
            set(value) {
                renderState.framesPerUpdate = value
            }

        var autoPlay: Boolean
            get() = renderState.autoPlay
            set(value) {
                renderState.autoPlay = value
            }

        var speed: Float
            get() = renderState.speed
            set(value) {
                renderState.speed = value
            }

        val firstFrame: Int
            get() = renderState.firstFrame

        val lastFrame: Int
            get() = renderState.lastFrame

        var frameInterval: Long
            get() = renderState.frameInterval
            set(value) {
                renderState.frameInterval = value
            }

        constructor(copy: LottieDrawableState?) : this() {
            copy ?: return
            composition = copy.composition?.copy()
            width = copy.width
            height = copy.height
            repeatCount = copy.repeatCount
            repeatMode = copy.repeatMode
            framesPerUpdate = copy.framesPerUpdate
            autoPlay = copy.autoPlay
            speed = copy.speed
            setFrameBounds(copy.firstFrame, copy.lastFrame)
            frameInterval = copy.frameInterval
        }

        fun setFrameBounds(firstFrame: Int, lastFrame: Int?) {
            renderState.setFrameBounds(firstFrame, lastFrame)
        }

        fun releaseComposition() {
            composition?.release()
            composition = null
        }

        fun valid(): Boolean {
            return composition?.isValid() == true
        }

        fun setCompositionSize(width: Int, height: Int) {
            this.width = width
            this.height = height
            composition?.setSize(width, height)
        }

        fun renderFrame(frame: Int): Bitmap? {
            return composition?.renderFrame(frame)
        }

        override fun newDrawable(): Drawable {
            return LottieDrawable(this)
        }

        override fun getChangingConfigurations(): Int {
            return 0
        }
    }

    companion object {
        /**
         * Repeats playback without an end.
         */
        const val INFINITE = LottieConstants.INFINITE

        /**
         * Restarts from the first frame after reaching the end frame.
         */
        const val RESTART = LottieConstants.RESTART

        /**
         * Reverses the playback direction after reaching the end frame.
         */
        const val REVERSE = LottieConstants.REVERSE

        @JvmStatic
        fun fromRawResource(resources: Resources, @RawRes resId: Int): LottieDrawable {
            val drawable = LottieDrawable()
            drawable.lottieState.composition = LottieSwComposition.fromRawResource(resources, resId)
            drawable.setFrameBounds(drawable.firstFrame)
            return drawable
        }
    }
}
