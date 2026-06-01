package org.thorvg.view.lottie

enum class Renderer(internal val attrValue: Int) {
    Default(0),
    Sw(1),
    Gl(2);

    internal companion object {
        fun fromAttr(value: Int): Renderer = entries.firstOrNull { it.attrValue == value } ?: Default
    }
}
