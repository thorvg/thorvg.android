package org.thorvg.view.lottie

/**
 * Receives lifecycle callbacks from a ThorVG Lottie playback session.
 */
interface LottieAnimationListener {
    /**
     * Called when playback starts drawing the first frame.
     */
    fun onAnimationStart()

    /**
     * Called when playback reaches its final frame and does not repeat anymore.
     */
    fun onAnimationEnd()

    /**
     * Called when playback wraps and starts a new iteration.
     */
    fun onAnimationRepeat()
}