package org.thorvg.core.lottie

/**
 * Shared playback constants used by ThorVG Lottie adapters.
 */
object LottieConstants {
    /**
     * Repeats playback without an end.
     */
    const val INFINITE = -1

    /**
     * Restarts from the first frame after reaching the end frame.
     */
    const val RESTART = 1

    /**
     * Reverses the playback direction after reaching the end frame.
     */
    const val REVERSE = 2
}