package org.thorvg.lottie;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.thorvg.lottie.LottieDrawable.LottieAnimationListener;

public class LottieAnimationView extends View {

    private LottieDrawable mDrawable;
    private LottieAnimationListener mListener;

    private int mResId = Resources.ID_NULL;

    private boolean mOnAttached = false;

    public LottieAnimationView(Context context) {
        this(context, null);
    }

    public LottieAnimationView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LottieAnimationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public LottieAnimationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.LottieAnimationView, defStyleAttr, defStyleRes);
        mResId = a.getResourceId(R.styleable.LottieAnimationView_lottieDrawable, mResId);
        a.recycle();
    }

    public void setLottieDrawableResource(@DrawableRes int resId) {
        if (mResId != resId) {
            mResId = resId;

            if (mDrawable != null) {
                mDrawable.release();
            }

            createLottieDrawable();
        }
    }

    private void createLottieDrawable() {
        if (mOnAttached && mResId != Resources.ID_NULL && mDrawable == null) {
            mDrawable = LottieDrawable.create(getContext().getResources(), mResId);
            if (mDrawable != null) {
                mDrawable.setCallback(this);
                mDrawable.setAnimationListener(mListener);
                if (mDrawable.isAutoPlay()) {
                    startAnimation();
                }
            }
        }
    }

    public void setSize(int width, int height) {
        if (mDrawable != null) {
            mDrawable.setSize(width, height);
        }
    }

    public void startAnimation() {
        if (mDrawable != null) {
            mDrawable.start();
        }
    }

    public void stopAnimation() {
        if (mDrawable != null) {
            mDrawable.stop();
        }
    }

    public void pauseAnimation() {
        if (mDrawable != null) {
            mDrawable.pause();
        }
    }

    public void resumeAnimation() {
        if (mDrawable != null) {
            mDrawable.resume();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mDrawable != null) {
            mDrawable.setSize(getMeasuredWidth(), getMeasuredHeight());
        }
    }

    @Override
    protected void onAttachedToWindow() {
        mOnAttached = true;

        super.onAttachedToWindow();

        createLottieDrawable();
    }

    @Override
    protected void onDetachedFromWindow() {
        mOnAttached = false;

        super.onDetachedFromWindow();

        if (mDrawable != null) {
            mDrawable.release();
            mDrawable = null;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        if (mDrawable != null) {
            mDrawable.draw(canvas);
        }
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable drawable) {
        super.invalidateDrawable(drawable);
        invalidate();
    }

    public void setAnimationListener(LottieAnimationListener listener) {
        mListener = listener;
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        return super.onSaveInstanceState();
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
    }
}