package org.thorvg.core.lottie

import androidx.annotation.IntDef

/**
 * Declares the supported repeat modes for ThorVG Lottie playback.
 */
@IntDef(LottieConstants.RESTART, LottieConstants.REVERSE)
@Retention(AnnotationRetention.SOURCE)
annotation class LottieRepeatMode