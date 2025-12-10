package org.thorvg.lottie

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
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
import androidx.annotation.IntDef
import androidx.annotation.RawRes
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class LottieDrawable internal constructor() : Drawable(), Animatable {

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef(RESTART, REVERSE)
    @Retention(AnnotationRetention.SOURCE)
    annotation class RepeatMode

    private var lottieState: LottieDrawableState = LottieDrawableState()

    /**
     * An animation listener to be notified when the animation starts, ends or repeats.
     */
    private var listener: LottieAnimationListener? = null

    /**
     * Additional playing state to indicate whether an animator has been start()'d. There is
     * some lag between a call to start() and the first animation frame. We should still note
     * that the animation has been started, even if it's first animation frame has not yet
     * happened, and reflect that state in isRunning().
     * Note that delayed animations are different: they are not started until their first
     * animation frame, which occurs after their delay elapses.
     */
    private var isRunning = false

    /**
     * Set to true when the animation ends.
     */
    private var isEnded = false

    /**
     * Set to true when the animation starts.
     */
    private var isStarted = false

    /**
     * Indicates how many times the animation was repeated.
     */
    private var repeated = 0

    private var frame = 0

    /**
     * Animation handler used to schedule updates.
     */
    private val handler = Handler(Looper.getMainLooper())

    private val nextFrameRunnable = Runnable { invalidateSelf() }

    // Temp variable, only for saving "new" operation at the draw() time.
    private val tmpPaint = Paint()

    private var mutated = false

    internal constructor(state: LottieDrawableState) : this() {
        lottieState = state
    }

    fun release() {
        lottieState.releaseLottie()
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

            if (repeated == lottieState.repeatCount) {
                if (!isEnded) {
                    isEnded = true
                    dispatchAnimationEnd()
                }
            } else {
                var resetFrame = false
                // Increase frame count.
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

            val endTime = System.nanoTime()

            handler.postDelayed(
                nextFrameRunnable, lottieState.frameInterval
                        - ((endTime - startTime) / 1000000)
            )
        }
    }

    fun getFrame(frame: Int): Bitmap? {
        return lottieState.getLottieBuffer(frame)
    }

    override fun setAlpha(alpha: Int) {
    }

    @Deprecated("Deprecated in Drawable")
    override fun setColorFilter(colorFilter: ColorFilter?) {
    }

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
     * Defines how many times the animation should be repeated. If the repeat
     * count is 0, the animation is never repeated. If the repeat count is
     * greater than 0 or [INFINITE], the repeat mode will be taken
     * into account. The repeat count is 0 by default.
     *
     * @property repeatCount the number of times the animation should be repeated
     */
    var repeatCount: Int
        get() = lottieState.repeatCount
        set(count) {
            lottieState.repeatCount = count
            repeated = 0
        }

    /**
     * Defines what this animation should do when it reaches the end. This
     * setting is applied only when the repeat count is either greater than
     * 0 or [INFINITE]. Defaults to [RESTART].
     *
     * @param mode [RESTART] or [REVERSE]
     */
    fun setRepeatMode(@RepeatMode mode: Int) {
        lottieState.repeatMode = mode
    }

    /**
     * Defines what this animation should do when it reaches the end.
     *
     * @return either one of [REVERSE] or [RESTART]
     */
    @get:RepeatMode
    val repeatMode: Int
        get() = lottieState.repeatMode

    fun setFirstFrame(frame: Int) {
        lottieState.firstFrame = frame
    }

    val firstFrame: Int
        get() = lottieState.firstFrame

    fun setLastFrame(frame: Int) {
        lottieState.lastFrame = frame
    }

    val lastFrame: Int
        get() = lottieState.lastFrame

    val isAutoPlay: Boolean
        get() = lottieState.autoPlay

    /**
     * Gets the length of the animation.
     *
     * @return The length of the animation, in milliseconds.
     */
    val duration: Long
        get() = if (lottieState.valid()) lottieState.lottie?.duration ?: 0L else 0L

    var speed: Float
        @FloatRange(from = 0.0)
        get() = lottieState.speed
        set(@FloatRange(from = 0.0) value) {
            lottieState.speed = value
        }

    fun setSize(width: Int, height: Int) {
        require(width > 0) { "LottieDrawable requires width > 0" }
        require(height > 0) { "LottieDrawable requires height > 0" }
        lottieState.setLottieSize(width, height)
    }

    override fun isRunning(): Boolean {
        return isRunning
    }

    override fun start() {
        isRunning = true
        isEnded = false
        isStarted = false
        repeated = 0
        frame = lottieState.firstFrame
        invalidateSelf()
    }

    override fun stop() {
        isRunning = false
        handler.removeCallbacks(nextFrameRunnable)
    }

    fun pause() {
        isRunning = false
        handler.removeCallbacks(nextFrameRunnable)
    }

    fun resume() {
        isRunning = true
        invalidateSelf()
    }

    /**
     * Binds an lottie animation listener to this drawable. The lottie animation listener
     * is notified of animation events such as the end of the animation or the repetition of
     * the animation.
     *
     * @param listener the lottie animation listener to be notified
     */
    fun setAnimationListener(listener: LottieAnimationListener?) {
        this@LottieDrawable.listener = listener
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
        r: Resources, parser: XmlPullParser,
        attrs: AttributeSet
    ) {
        val state = lottieState

        val a = r.obtainAttributes(attrs, R.styleable.LottieDrawable)

        val rawRes = a.getResourceId(R.styleable.LottieDrawable_rawRes, 0)
        require(rawRes != 0) { "" }

        state.lottie = Lottie(loadJSONFile(r, rawRes))
        state.lottie?.let { lottie -> setLastFrame(a.getInt(R.styleable.LottieDrawable_frameTo, lottie.frameCount)) }
        setFirstFrame(a.getInt(R.styleable.LottieDrawable_frameFrom, 0))
        this@LottieDrawable.speed = a.getFloat(R.styleable.LottieDrawable_speed, 1f)

        setRepeatMode(a.getInt(R.styleable.LottieDrawable_android_repeatMode, RESTART))
        this@LottieDrawable.repeatCount =
            a.getInt(R.styleable.LottieDrawable_android_repeatCount, 0)

        state.autoPlay = a.getBoolean(R.styleable.LottieDrawable_android_autoStart, true)

        val defaultSize = (r.displayMetrics.density * UNDEFINED_SIZE_IN_DIP).toInt()
        state.baseWidth = a.getDimensionPixelOffset(
            R.styleable.LottieDrawable_android_width,
            defaultSize
        ).toFloat()
        state.baseHeight = a.getDimensionPixelOffset(
            R.styleable.LottieDrawable_android_height,
            defaultSize
        ).toFloat()

        a.recycle()

        state.setLottieSize(state.baseWidth.toInt(), state.baseHeight.toInt())
    }

    internal class LottieDrawableState : ConstantState {
        var lottie: Lottie? = null

        var baseWidth = 0f
        var baseHeight = 0f

        var width = 0
        var height = 0

        /**
         * The type of repetition that will occur when repeatMode is nonzero. RESTART means the
         * animation will start from the beginning on every new cycle. REVERSE means the animation
         * will reverse directions on each iteration.
         */
        var repeatMode = RESTART
            set(@RepeatMode value) {
                field = value
                framesPerUpdate = if (field == RESTART) 1 else -1
            }

        var repeatCount = 0

        var speed = 0f
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
                 lottie?.apply {
                     field = value.coerceAtMost(frameCount)
                     updateFrameInterval()
                }
            }

        var frameInterval = 0L

        var framesPerUpdate = 1

        var autoPlay = false

        var alpha = 1f

        constructor()

        constructor(copy: LottieDrawableState?) {
            copy?.lottie?.let { lottie ->
                this@LottieDrawableState.lottie = Lottie(lottie)
                baseWidth = copy.baseWidth
                baseHeight = copy.baseHeight
                repeatCount = copy.repeatCount
                repeatMode = copy.repeatMode
                framesPerUpdate = copy.framesPerUpdate
                autoPlay = copy.autoPlay
                speed = copy.speed
                alpha = copy.alpha
            }
        }

        fun releaseLottie() {
            lottie?.destroy()
            lottie = null
        }

        fun valid(): Boolean {
            return lottie?.nativePtr != 0L
        }

        fun setLottieSize(width: Int, height: Int) {
            if (width != this@LottieDrawableState.width || height != this@LottieDrawableState.height) {
                this@LottieDrawableState.width = width
                this@LottieDrawableState.height = height
                lottie?.setBufferSize(width, height)
            }
        }

        fun getLottieBuffer(frame: Int): Bitmap? {
            return lottie?.getBuffer(frame)
        }

        private fun updateFrameInterval() {
            lottie?.let {
                val frameCount = lastFrame - firstFrame
                frameInterval = (it.duration / frameCount / speed).toLong()
            }
        }

        override fun newDrawable(): Drawable {
            return LottieDrawable(this)
        }

        override fun getChangingConfigurations(): Int {
            return 0
        }
    }

    internal class Lottie {
        val jsonContent: String
        val nativePtr: Long
        var frameCount: Int = 0
        var duration: Long = 0
        private var buffer: Bitmap? = null

        constructor(copy: Lottie) {
            jsonContent = copy.jsonContent
            nativePtr = init(copy.jsonContent)
            frameCount = copy.frameCount
            duration = copy.duration
        }

        constructor(content: String) {
            jsonContent = content
            nativePtr = init(jsonContent)
        }

        private fun init(content: String): Long {
            val outValues = IntArray(LOTTIE_INFO_COUNT)
            val nativePtr = nCreateLottie(content, content.length, outValues)
            frameCount = outValues[LOTTIE_INFO_FRAME_COUNT]
            duration = outValues[LOTTIE_INFO_DURATION] * 1000L
            return nativePtr
        }

        fun destroy() {
            buffer?.recycle()
            buffer = null
            nDestroyLottie(nativePtr)
        }

        fun setBufferSize(width: Int, height: Int) {
            buffer = createBitmap(width, height, Bitmap.Config.ARGB_8888)
            nSetLottieBufferSize(nativePtr, buffer, width.toFloat(), height.toFloat())
        }

        fun getBuffer(frame: Int): Bitmap? {
            buffer?.let {
                nDrawLottieFrame(nativePtr, it, frame)
            }
            return buffer
        }

        companion object {
            private const val LOTTIE_INFO_FRAME_COUNT = 0
            private const val LOTTIE_INFO_DURATION = 1
            private const val LOTTIE_INFO_COUNT = 2
        }
    }

    /**
     * An animation listener receives notifications from an animation.
     * Notifications indicate animation related events, such as the end or the
     * repetition of the animation.
     */
    interface LottieAnimationListener {
        /**
         * Notifies the start of the lottie animation.
         */
        fun onAnimationStart()

        /**
         * Notifies the end of the lottie animation. This callback is not invoked
         * for animations with repeat count set to INFINITE.
         */
        fun onAnimationEnd()

        /**
         * Notifies the repetition of the lottie animation.
         */
        fun onAnimationRepeat()
    }

    companion object {
        private const val TAG = "LottieDrawable"
        private const val UNDEFINED_SIZE_IN_DIP = 50

        /**
         * Repeat the animation indefinitely.
         */
        const val INFINITE = -1

        /**
         * When the animation reaches the end and the repeat count is INFINTE_REPEAT
         * or a positive value, the animation restarts from the beginning.
         */
        const val RESTART = 1

        /**
         * When the animation reaches the end and the repeat count is INFINTE_REPEAT
         * or a positive value, the animation plays backward (and then forward again).
         */
        const val REVERSE = 2

        init {
            System.loadLibrary("lottie-libs")
        }

        @JvmStatic
        fun create(resources: Resources, @DrawableRes resId: Int): LottieDrawable? {
            return try {
                @SuppressLint("ResourceType")
                val parser = resources.getXml(resId)
                val attrs = Xml.asAttributeSet(parser)
                var type: Int
                while (parser.next().also { type = it } != XmlPullParser.START_TAG
                    && type != XmlPullParser.END_DOCUMENT
                ) {
                    // Empty loop
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

        @Throws(IOException::class)
        private fun loadJSONFile(res: Resources, @RawRes resId: Int): String {
            return try {
                res.openRawResource(resId).use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        reader.lineSequence().joinToString("")
                    }
                }
            } catch (e: IOException) {
                throw IOException("Failed to read a lottie file.")
            }
        }

        @JvmStatic
        private external fun nCreateLottie(
            content: String?,
            length: Int,
            outValues: IntArray?
        ): Long

        @JvmStatic
        private external fun nSetLottieBufferSize(
            lottiePtr: Long,
            bitmap: Bitmap?,
            width: Float,
            height: Float
        )

        @JvmStatic
        private external fun nDrawLottieFrame(lottiePtr: Long, bitmap: Bitmap, frame: Int)

        @JvmStatic
        private external fun nDestroyLottie(lottiePtr: Long)
    }
}