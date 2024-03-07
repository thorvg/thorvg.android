package org.thorvg.lottie;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import androidx.annotation.DrawableRes;
import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.annotation.RestrictTo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class LottieDrawable extends Drawable implements Animatable {
    private static final String TAG = "LottieDrawable";

    private static final int UNDEFINED_SIZE_IN_DIP = 50;

    /**
     * Repeat the animation indefinitely.
     */
    public static final int INFINITE = -1;
    /**
     * When the animation reaches the end and the repeat count is INFINTE_REPEAT
     * or a positive value, the animation restarts from the beginning.
     */
    public static final int RESTART = 1;
    /**
     * When the animation reaches the end and the repeat count is INFINTE_REPEAT
     * or a positive value, the animation plays backward (and then forward again).
     */
    public static final int REVERSE = 2;

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef({RESTART, REVERSE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RepeatMode {}

    private LottieDrawableState mLottieState;

    /**
     * An animation listener to be notified when the animation starts, ends or repeats.
     */
    private LottieAnimationListener mListener;

    /**
     * Additional playing state to indicate whether an animator has been start()'d. There is
     * some lag between a call to start() and the first animation frame. We should still note
     * that the animation has been started, even if it's first animation frame has not yet
     * happened, and reflect that state in isRunning().
     * Note that delayed animations are different: they are not started until their first
     * animation frame, which occurs after their delay elapses.
     */
    private boolean mRunning;

    /**
     * Set to true when the animation ends.
     */
    private boolean mEnded = false;

    /**
     * Set to true when the animation starts.
     */
    private boolean mStarted = false;

    /**
     * Indicates how many times the animation was repeated.
     */
    private int mRepeated = 0;

    private int mFrame;

    /**
     * Animation handler used to schedule updates.
     */
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final Runnable mNextFrameRunnable = () -> invalidateSelf();

    // Temp variable, only for saving "new" operation at the draw() time.
    private Paint mTmpPaint = new Paint();

    private boolean mMutated;

    private LottieDrawable() {
        mLottieState = new LottieDrawableState();
    }

    private LottieDrawable(@NonNull LottieDrawableState state) {
        mLottieState = state;
    }

    public void release() {
        mLottieState.releaseLottie();
    }

    @NonNull
    @Override
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mLottieState = new LottieDrawableState(mLottieState);
            mMutated = true;
        }
        return this;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mLottieState.valid() && mRunning) {
            if (!mStarted) {
                mStarted = true;
                dispatchAnimationStart();
            }

            long startTime = System.nanoTime();

            canvas.drawBitmap(getFrame(mFrame), 0, 0, mTmpPaint);

            if (mRepeated == mLottieState.mRepeatCount) {
                if (!mEnded) {
                    mEnded = true;
                    dispatchAnimationEnd();
                }
            } else {
                boolean resetFrame = false;
                // Increase frame count.
                mFrame += mLottieState.mFramesPerUpdate;
                if (mFrame > mLottieState.mLastFrame) {
                    mFrame = mLottieState.mFirstFrame;
                    resetFrame = true;
                } else if (mFrame < mLottieState.mFirstFrame) {
                    mFrame = mLottieState.mLastFrame;
                    resetFrame = true;
                }
                if (resetFrame) {
                    mRepeated++;
                    dispatchAnimationRepeat();
                }
            }

            long endTime = System.nanoTime();

            mHandler.postDelayed(mNextFrameRunnable, mLottieState.mFrameInterval
                    - ((endTime - startTime) / 1000000));
        }
    }

    @Nullable
    public Bitmap getFrame(int frame) {
        return mLottieState.getLottieBuffer(frame);
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return mLottieState.mWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mLottieState.mHeight;
    }

    /**
     * Sets how many times the animation should be repeated. If the repeat
     * count is 0, the animation is never repeated. If the repeat count is
     * greater than 0 or {@link #INFINITE}, the repeat mode will be taken
     * into account. The repeat count is 0 by default.
     *
     * @param count the number of times the animation should be repeated
     */
    public void setRepeatCount(int count) {
        mLottieState.mRepeatCount = count;
        mRepeated = 0;
    }

    /**
     * Defines how many times the animation should repeat. The default value
     * is 0.
     *
     * @return the number of times the animation should repeat, or {@link #INFINITE}
     */
    public int getRepeatCount() {
        return mLottieState.mRepeatCount;
    }

    /**
     * Defines what this animation should do when it reaches the end. This
     * setting is applied only when the repeat count is either greater than
     * 0 or {@link #INFINITE}. Defaults to {@link #RESTART}.
     *
     * @param mode {@link #RESTART} or {@link #REVERSE}
     */
    public void setRepeatMode(@RepeatMode int mode) {
        mLottieState.setRepeatMode(mode);
    }

    /**
     * Defines what this animation should do when it reaches the end.
     *
     * @return either one of {@link #REVERSE} or {@link #RESTART}
     */
    @RepeatMode
    public int getRepeatMode() {
        return mLottieState.mRepeatMode;
    }

    public void setFirstFrame(int frame) {
        mLottieState.setFirstFrame(frame);
    }

    public int getFirstFrame() {
        return mLottieState.mFirstFrame;
    }

    public void setLastFrame(int frame) {
        mLottieState.setLastFrame(frame);
    }

    public int getLastFrame() {
        return mLottieState.mLastFrame;
    }

    public boolean isAutoPlay() {
        return mLottieState.mAutoPlay;
    }

    /**
     * Gets the length of the animation.
     *
     * @return The length of the animation, in milliseconds.
     */
    public long getDuration() {
        return mLottieState.mLottie.mDuration;
    }

    public void setSpeed(@FloatRange(from = 0) float speed) {
        mLottieState.setSpeed(speed);
    }

    @FloatRange(from = 0)
    public float getSpeed() {
        return mLottieState.mSpeed;
    }

    public void setSize(int width, int height) {
        if (width <= 0) {
            throw new IllegalArgumentException("LottieDrawable requires width > 0");
        } else if (height <= 0) {
            throw new IllegalArgumentException("LottieDrawable requires height > 0");
        }
        mLottieState.setLottieSize(width, height);
    }

    @Override
    public boolean isRunning() {
        return mRunning;
    }

    @Override
    public void start() {
        mRunning = true;
        mEnded = false;
        mStarted = false;
        mRepeated = 0;
        mFrame = mLottieState.mFirstFrame;
        invalidateSelf();
    }

    @Override
    public void stop() {
        mRunning = false;
        mHandler.removeCallbacks(mNextFrameRunnable);
    }

    public void pause() {
        mRunning = false;
        mHandler.removeCallbacks(mNextFrameRunnable);
    }

    public void resume() {
        mRunning = true;
        invalidateSelf();
    }

    /**
     * <p>Binds an lottie animation listener to this drawable. The lottie animation listener
     * is notified of animation events such as the end of the animation or the repetition of
     * the animation.</p>
     *
     * @param listener the lottie animation listener to be notified
     */
    public void setAnimationListener(LottieAnimationListener listener) {
        mListener = listener;
    }

    void dispatchAnimationStart() {
        if (mListener != null) {
            mListener.onAnimationStart();
        }
    }

    void dispatchAnimationRepeat() {
        if (mListener != null) {
            mListener.onAnimationRepeat();
        }
    }

    void dispatchAnimationEnd() {
        if (mListener != null) {
            mListener.onAnimationEnd();
        }
    }

    @Nullable
    public static LottieDrawable create(Resources resources, @DrawableRes int resId) {
        try {
            @SuppressLint("ResourceType") final XmlPullParser parser = resources.getXml(resId);
            final AttributeSet attrs = Xml.asAttributeSet(parser);
            int type;
            while ((type = parser.next()) != XmlPullParser.START_TAG
                    && type != XmlPullParser.END_DOCUMENT) {
                // Empty loop
            }
            if (type != XmlPullParser.START_TAG) {
                throw new XmlPullParserException("No start tag found");
            }

            final LottieDrawable drawable = new LottieDrawable();
            drawable.inflate(resources, parser, attrs);

            return drawable;
        } catch (XmlPullParserException e) {
            Log.e(TAG, "parser error", e);
        } catch (IOException e) {
            Log.e(TAG, "parser error", e);
        }
        return null;
    }

    @Override
    public void inflate(@NonNull Resources r, @NonNull XmlPullParser parser,
            @NonNull AttributeSet attrs) throws IOException {
        final LottieDrawableState state = mLottieState;

        final TypedArray a = r.obtainAttributes(attrs, R.styleable.LottieDrawable);

        final int rawRes = a.getResourceId(R.styleable.LottieDrawable_rawRes, 0);
        if (rawRes == 0) {
            throw new IllegalArgumentException("");
        }

        state.mLottie = new Lottie(loadJSONFile(r, rawRes));

        setLastFrame(a.getInt(R.styleable.LottieDrawable_frameTo, state.mLottie.mFrameCount));
        setFirstFrame(a.getInt(R.styleable.LottieDrawable_frameFrom, 0));
        setSpeed(a.getFloat(R.styleable.LottieDrawable_speed, 1f));

        setRepeatMode(a.getInt(R.styleable.LottieDrawable_android_repeatMode, RESTART));
        setRepeatCount(a.getInt(R.styleable.LottieDrawable_android_repeatCount, 0));

        state.mAutoPlay = a.getBoolean(R.styleable.LottieDrawable_android_autoStart, true);

        final int defaultSize = (int) (r.getDisplayMetrics().density * UNDEFINED_SIZE_IN_DIP);
        state.mBaseWidth = a.getDimensionPixelOffset(R.styleable.LottieDrawable_android_width,
                defaultSize);
        state.mBaseHeight = a.getDimensionPixelOffset(R.styleable.LottieDrawable_android_height,
                defaultSize);

        a.recycle();

        state.setLottieSize((int) state.mBaseWidth, (int) state.mBaseHeight);
    }

    private static String loadJSONFile(Resources res, @RawRes int resId) throws IOException {
        String json = null;
        try {
            InputStream is = res.openRawResource(resId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            is.close();
            json = sb.toString();
        } catch (IOException e) {
            throw new IOException("Failed to read a lottie file.");
        }
        return json;
    }

    private static class LottieDrawableState extends ConstantState {
        private Lottie mLottie;

        private float mBaseWidth = 0;
        private float mBaseHeight = 0;

        private int mWidth = 0;
        private int mHeight = 0;

        /**
         * The type of repetition that will occur when repeatMode is nonzero. RESTART means the
         * animation will start from the beginning on every new cycle. REVERSE means the animation
         * will reverse directions on each iteration.
         */
        private int mRepeatMode = RESTART;

        private int mRepeatCount;

        private float mSpeed;

        private int mFirstFrame;
        private int mLastFrame;

        private long mFrameInterval;

        private int mFramesPerUpdate = 1;

        private boolean mAutoPlay;

        private float mAlpha = 1f;

        LottieDrawableState(LottieDrawableState copy) {
            if (copy != null) {
                mLottie = new Lottie(copy.mLottie);
                mBaseWidth = copy.mBaseWidth;
                mBaseHeight = copy.mBaseHeight;
                mRepeatCount = copy.mRepeatCount;
                mRepeatMode = copy.mRepeatMode;
                mFramesPerUpdate = copy.mFramesPerUpdate;
                mAutoPlay = copy.mAutoPlay;
                mSpeed = copy.mSpeed;
                mAlpha = copy.mAlpha;
            }
        }

        private void releaseLottie() {
            mLottie.destroy();
            mLottie = null;
        }

        private boolean valid() {
            return mLottie != null && mLottie.mNativePtr != 0;
        }

        private void setLottieSize(int width, int height) {
            if (width != mWidth || height != mHeight) {
                mWidth = width;
                mHeight = height;
                mLottie.setBufferSize(width, height);
            }
        }

        private Bitmap getLottieBuffer(int frame) {
            return mLottie.getBuffer(frame);
        }

        private void setRepeatMode(@RepeatMode int mode) {
            mRepeatMode = mode;
            mFramesPerUpdate = mRepeatMode == RESTART ? 1 : -1;
        }

        private void setSpeed(@FloatRange(from = 0) float speed) {
            mSpeed = speed;
            updateFrameInterval();
        }

        private void setFirstFrame(int frame) {
            mFirstFrame = Math.min(frame, mLastFrame);
            updateFrameInterval();
        }

        private void setLastFrame(int frame) {
            mLastFrame = Math.min(frame, mLottie.mFrameCount);
            updateFrameInterval();
        }

        private void updateFrameInterval() {
            int frameCount = mLastFrame - mFirstFrame;
            mFrameInterval = (long) (mLottie.mDuration / frameCount / mSpeed);
        }

        LottieDrawableState() {
        }

        @NonNull
        @Override
        public Drawable newDrawable() {
            return new LottieDrawable(this);
        }

        @Override
        public int getChangingConfigurations() {
            return 0;
        }
    }

    private static class Lottie {
        private static final int LOTTIE_INFO_FRAME_COUNT = 0;
        private static final int LOTTIE_INFO_DURATION = 1;
        private static final int LOTTIE_INFO_COUNT = 2;

        private final String mJsonContent;

        private final long mNativePtr;

        private int mFrameCount;

        // How long the animation should last in ms
        private long mDuration;

        private Bitmap mBuffer;

        Lottie(@NonNull Lottie copy) {
            mJsonContent = copy.mJsonContent;
            mNativePtr = init(copy.mJsonContent);
            mFrameCount = copy.mFrameCount;
            mDuration = copy.mDuration;
        }

        Lottie(String content) {
            mJsonContent = content;
            mNativePtr = init(mJsonContent);
        }

        long init(@NonNull String content) {
            final int[] outValues = new int[LOTTIE_INFO_COUNT];
            long nativePtr = nCreateLottie(content, content.length(), outValues);
            mFrameCount = outValues[LOTTIE_INFO_FRAME_COUNT];
            mDuration = outValues[LOTTIE_INFO_DURATION] * 1000L;
            return nativePtr;
        }

        void destroy() {
            if (mBuffer != null) {
                mBuffer.recycle();
                mBuffer = null;
            }
            nDestroyLottie(mNativePtr);
        }

        void setBufferSize(int width, int height) {
            mBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            nSetLottieBufferSize(mNativePtr, mBuffer, width, height);
        }

        Bitmap getBuffer(int frame) {
            nDrawLottieFrame(mNativePtr, mBuffer, frame);
            return mBuffer;
        }
    }

    /**
     * <p>An animation listener receives notifications from an animation.
     * Notifications indicate animation related events, such as the end or the
     * repetition of the animation.</p>
     */
    public interface LottieAnimationListener {
        /**
         * <p>Notifies the start of the lottie animation.</p>
         */
        void onAnimationStart();

        /**
         * <p>Notifies the end of the lottie animation. This callback is not invoked
         * for animations with repeat count set to INFINITE.</p>
         */
        void onAnimationEnd();

        /**
         * <p>Notifies the repetition of the lottie animation.</p>
         */
        void onAnimationRepeat();
    }

    static {
        System.loadLibrary("lottie-libs");
    }

    private static native long nCreateLottie(String content, int length, int[] outValues);

    private static native void nSetLottieBufferSize(long lottiePtr, Bitmap bitmap, float width, float height);

    private static native void nDrawLottieFrame(long lottiePtr, Bitmap bitmap, int frame);

    private static native void nDestroyLottie(long lottiePtr);
}