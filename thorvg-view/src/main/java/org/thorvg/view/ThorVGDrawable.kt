package org.thorvg.view

import android.graphics.drawable.Drawable

/**
 * Base drawable contract for ThorVG-backed View adapters.
 */
abstract class ThorVGDrawable : Drawable() {
    /**
     * Releases any native resources owned by this drawable.
     */
    open fun release() = Unit

    /**
     * Resizes the underlying render target in pixels.
     */
    open fun setSize(width: Int, height: Int) = Unit
}
