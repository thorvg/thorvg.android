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

package org.thorvg.view.lottie

import android.annotation.SuppressLint
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
import android.util.AttributeSet
import android.util.Log
import android.util.Xml
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.annotation.RawRes
import org.thorvg.core.lottie.LottieConstants
import org.thorvg.core.lottie.LottieNativeBindings
import org.thorvg.core.lottie.LottieRepeatMode
import org.thorvg.view.R
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Drawable adapter that renders a ThorVG Lottie composition into an Android [Canvas].
 *
 * Instances are usually created from a drawable XML resource via [create].
 */
class LottieDrawable internal constructor() : Drawable(), Animatable {
    private var lottieState: LottieDrawableState = LottieDrawableState()
    private var listener: LottieAnimationListener? = null

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
    fun release() {
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
                var resetFrame = false
                frame += lottieState.framesPerUpdate
                if (frame > lottieState.lastFrame) {
                    frame = lottieState.firstFrame
                    resetFrame = true
                } else if (frame < lottieState.firstFrame) {
                    frame = lottieState.lastFrame
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
     * Sets the first frame that playback is allowed to render.
     */
    fun setFirstFrame(frame: Int) {
        lottieState.firstFrame = frame
    }

    /**
     * First frame used for playback.
     */
    val firstFrame: Int
        get() = lottieState.firstFrame

    /**
     * Sets the last frame that playback is allowed to render.
     */
    fun setLastFrame(frame: Int) {
        lottieState.lastFrame = frame
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
    fun setSize(width: Int, height: Int) {
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
    fun setAnimationListener(listener: LottieAnimationListener?) {
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

    override fun inflate(
        resources: Resources,
        parser: XmlPullParser,
        attrs: AttributeSet
    ) {
        val state = lottieState
        val attributes = resources.obtainAttributes(attrs, R.styleable.LottieDrawable)

        val rawRes = attributes.getResourceId(R.styleable.LottieDrawable_rawRes, 0)
        require(rawRes != 0) { "" }

        state.composition = ViewLottieComposition.fromRawResource(resources, rawRes)
        state.composition?.let { composition ->
            setLastFrame(attributes.getInt(R.styleable.LottieDrawable_frameTo, composition.frameCount))
        }
        setFirstFrame(attributes.getInt(R.styleable.LottieDrawable_frameFrom, 0))
        speed = attributes.getFloat(R.styleable.LottieDrawable_speed, 1f)
        setRepeatMode(attributes.getInt(R.styleable.LottieDrawable_android_repeatMode, RESTART))
        repeatCount = attributes.getInt(R.styleable.LottieDrawable_android_repeatCount, 0)
        state.autoPlay = attributes.getBoolean(R.styleable.LottieDrawable_android_autoStart, true)

        val defaultSize = (resources.displayMetrics.density * UNDEFINED_SIZE_IN_DIP).toInt()
        state.baseWidth = attributes.getDimensionPixelOffset(
            R.styleable.LottieDrawable_android_width,
            defaultSize
        ).toFloat()
        state.baseHeight = attributes.getDimensionPixelOffset(
            R.styleable.LottieDrawable_android_height,
            defaultSize
        ).toFloat()

        attributes.recycle()

        state.setCompositionSize(state.baseWidth.toInt(), state.baseHeight.toInt())
    }

    internal class LottieDrawableState() : ConstantState() {
        private val renderState = ViewLottieRenderState()

        var composition: ViewLottieComposition?
            get() = renderState.composition
            set(value) {
                renderState.composition = value
            }

        var baseWidth: Float
            get() = renderState.baseWidth
            set(value) {
                renderState.baseWidth = value
            }

        var baseHeight: Float
            get() = renderState.baseHeight
            set(value) {
                renderState.baseHeight = value
            }

        var width: Int
            get() = renderState.width
            set(value) {
                renderState.width = value
            }

        var height: Int
            get() = renderState.height
            set(value) {
                renderState.height = value
            }

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

        var firstFrame: Int
            get() = renderState.firstFrame
            set(value) {
                renderState.firstFrame = value
            }

        var lastFrame: Int
            get() = renderState.lastFrame
            set(value) {
                renderState.lastFrame = value
            }

        var frameInterval: Long
            get() = renderState.frameInterval
            set(value) {
                renderState.frameInterval = value
            }

        constructor(copy: LottieDrawableState?) : this() {
            copy ?: return
            composition = copy.composition?.copy()
            baseWidth = copy.baseWidth
            baseHeight = copy.baseHeight
            width = copy.width
            height = copy.height
            repeatCount = copy.repeatCount
            repeatMode = copy.repeatMode
            framesPerUpdate = copy.framesPerUpdate
            autoPlay = copy.autoPlay
            speed = copy.speed
            firstFrame = copy.firstFrame
            lastFrame = copy.lastFrame
            frameInterval = copy.frameInterval
        }

        fun releaseComposition() {
            renderState.releaseComposition()
        }

        fun valid(): Boolean {
            return renderState.valid()
        }

        fun setCompositionSize(width: Int, height: Int) {
            renderState.setCompositionSize(width, height)
        }

        fun renderFrame(frame: Int): Bitmap? {
            return renderState.renderFrame(frame)
        }

        override fun newDrawable(): Drawable {
            return LottieDrawable(this)
        }

        override fun getChangingConfigurations(): Int {
            return 0
        }
    }

    companion object {
        private const val TAG = "LottieDrawable"
        private const val UNDEFINED_SIZE_IN_DIP = 50

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

        /**
         * Creates a [LottieDrawable] from a drawable XML resource declared with `LottieDrawable` attributes.
         */
        @JvmStatic
        fun create(resources: Resources, @DrawableRes resId: Int): LottieDrawable? {
            return try {
                @SuppressLint("ResourceType")
                val parser = resources.getXml(resId)
                val attrs = Xml.asAttributeSet(parser)
                var type: Int
                while (parser.next().also { type = it } != XmlPullParser.START_TAG &&
                    type != XmlPullParser.END_DOCUMENT
                ) {
                    // Skip until start tag.
                }
                if (type != XmlPullParser.START_TAG) {
                    throw XmlPullParserException("No start tag found")
                }

                val drawable = LottieDrawable()
                drawable.inflate(resources, parser, attrs)
                drawable
            } catch (e: XmlPullParserException) {
                Log.e(TAG, "parser error", e)
                null
            } catch (e: IOException) {
                Log.e(TAG, "parser error", e)
                null
            }
        }
    }
}

internal class ViewLottieComposition private constructor(
    private val nativeLottie: NativeLottie
) {
    constructor(content: String) : this(NativeLottie(content))

    val frameCount: Int
        get() = nativeLottie.frameCount

    val duration: Long
        get() = nativeLottie.duration

    fun setSize(width: Int, height: Int) {
        require(width > 0) { "ViewLottieComposition requires width > 0" }
        require(height > 0) { "ViewLottieComposition requires height > 0" }
        nativeLottie.setBufferSize(width, height)
    }

    fun renderFrame(frame: Int): Bitmap? {
        return nativeLottie.getBuffer(frame)
    }

    fun release() {
        nativeLottie.destroy()
    }

    fun copy(): ViewLottieComposition {
        return ViewLottieComposition(nativeLottie.copy())
    }

    fun isValid(): Boolean {
        return nativeLottie.nativePtr != 0L
    }

    companion object {
        @JvmStatic
        fun fromRawResource(resources: Resources, @RawRes resId: Int): ViewLottieComposition {
            return ViewLottieComposition(loadJsonFile(resources, resId))
        }

        @Throws(IOException::class)
        private fun loadJsonFile(resources: Resources, @RawRes resId: Int): String {
            return try {
                resources.openRawResource(resId).use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        reader.lineSequence().joinToString("")
                    }
                }
            } catch (e: IOException) {
                throw IOException("Failed to read a lottie file.")
            }
        }
    }
}

private class ViewLottieRenderState {
    var composition: ViewLottieComposition? = null

    var baseWidth = 0f
    var baseHeight = 0f
    var width = 0
    var height = 0

    var repeatMode = LottieConstants.RESTART
        set(@LottieRepeatMode value) {
            field = value
            framesPerUpdate = if (field == LottieConstants.RESTART) 1 else -1
        }

    var repeatCount = 0
    var speed = 1f
        set(@FloatRange(from = 0.0) value) {
            field = value
            updateFrameInterval()
        }

    var firstFrame = 0
        set(value) {
            field = value.coerceAtMost(lastFrame)
            updateFrameInterval()
        }

    var lastFrame = 0
        set(value) {
            composition?.let {
                field = value.coerceAtMost(it.frameCount)
                updateFrameInterval()
            }
        }

    var frameInterval = 0L
    var framesPerUpdate = 1
    var autoPlay = false

    fun releaseComposition() {
        composition?.release()
        composition = null
    }

    fun valid(): Boolean {
        return composition?.isValid() == true
    }

    fun setCompositionSize(width: Int, height: Int) {
        if (width != this.width || height != this.height) {
            this.width = width
            this.height = height
            composition?.setSize(width, height)
        }
    }

    fun renderFrame(frame: Int): Bitmap? {
        return composition?.renderFrame(frame)
    }

    private fun updateFrameInterval() {
        val currentComposition = composition ?: return
        val totalFrames = lastFrame - firstFrame
        frameInterval = when {
            totalFrames <= 0 -> 0L
            speed <= 0f -> 0L
            else -> (currentComposition.duration / totalFrames / speed).toLong()
        }
    }
}

private class NativeLottie {
    val jsonContent: String
    val nativePtr: Long
    var frameCount: Int = 0
    var duration: Long = 0
    private var buffer: Bitmap? = null

    constructor(copy: NativeLottie) {
        jsonContent = copy.jsonContent
        nativePtr = init(copy.jsonContent)
        frameCount = copy.frameCount
        duration = copy.duration
    }

    constructor(content: String) {
        jsonContent = content
        nativePtr = init(jsonContent)
    }

    fun copy(): NativeLottie {
        return NativeLottie(this)
    }

    fun destroy() {
        buffer?.recycle()
        buffer = null
        LottieNativeBindings.nDestroyLottie(nativePtr)
    }

    fun setBufferSize(width: Int, height: Int) {
        buffer?.recycle()
        buffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        LottieNativeBindings.nSetLottieBufferSize(nativePtr, buffer, width.toFloat(), height.toFloat())
    }

    fun getBuffer(frame: Int): Bitmap? {
        buffer?.let {
            LottieNativeBindings.nDrawLottieFrame(nativePtr, it, frame)
        }
        return buffer
    }

    private fun init(content: String): Long {
        val outValues = IntArray(2)
        val pointer = LottieNativeBindings.nCreateLottie(content, content.length, outValues)
        frameCount = outValues[0]
        duration = outValues[1] * 1000L
        return pointer
    }
}
