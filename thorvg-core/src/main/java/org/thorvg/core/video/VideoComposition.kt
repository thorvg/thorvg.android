package org.thorvg.core.video

import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap

class VideoComposition private constructor(
    private val nativeVideo: NativeVideo
) {
    constructor(path: String) : this(NativeVideo(path))
    constructor(data: ByteArray, size: Int) : this(NativeVideo(data, size))

    val width: Int
        get() = nativeVideo.width

    val height: Int
        get() = nativeVideo.height

    val durationMillis: Long
        get() = nativeVideo.durationMillis

    val currentTimeMillis: Long
        get() = nativeVideo.currentTimeMillis

    fun setSize(width: Int, height: Int) {
        nativeVideo.setBufferSize(width, height)
    }

    fun render(): Bitmap? {
        return nativeVideo.render()
    }

    fun play() {
        nativeVideo.play()
    }

    fun pause() {
        nativeVideo.pause()
    }

    fun stop() {
        nativeVideo.stop()
    }

    fun seekTo(timeMillis: Long) {
        nativeVideo.seekTo(timeMillis.coerceIn(0L, durationMillis.coerceAtLeast(0L)))
    }

    fun loop(loop: Boolean) {
        nativeVideo.loop(loop)
    }

    fun volume(volume: Float) {
        nativeVideo.volume(volume.coerceIn(0f, 1f))
    }

    fun mute(muted: Boolean) {
        nativeVideo.mute(muted)
    }

    fun release() {
        nativeVideo.destroy()
    }

    fun isValid(): Boolean = nativeVideo.nativePtr != 0L
}

private class NativeVideo {
    var nativePtr: Long
        private set
    var width = 0
        private set
    var height = 0
        private set
    var durationMillis = 0L
        private set
    val currentTimeMillis: Long
        get() = if (nativePtr == 0L) 0L else VideoNativeBindings.nGetVideoTime(nativePtr)

    private var buffer: Bitmap? = null

    constructor(path: String) {
        val outValues = IntArray(VIDEO_INFO_COUNT)
        nativePtr = VideoNativeBindings.nCreateVideo(path, outValues)
        update(outValues)
    }

    constructor(data: ByteArray, size: Int) {
        val outValues = IntArray(VIDEO_INFO_COUNT)
        nativePtr = VideoNativeBindings.nCreateVideoData(data, size, outValues)
        update(outValues)
    }

    private fun update(outValues: IntArray) {
        width = outValues[VIDEO_INFO_WIDTH]
        height = outValues[VIDEO_INFO_HEIGHT]
        durationMillis = outValues[VIDEO_INFO_DURATION].toLong()
    }

    fun setBufferSize(width: Int, height: Int) {
        if (nativePtr == 0L || width <= 0 || height <= 0) return

        val existing = buffer
        if (existing != null &&
            !existing.isRecycled &&
            existing.width == width &&
            existing.height == height
        ) {
            return
        }

        val newBuffer = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        buffer = newBuffer
        VideoNativeBindings.nSetVideoBufferSize(nativePtr, newBuffer, width.toFloat(), height.toFloat())
    }

    fun render(): Bitmap? {
        if (nativePtr == 0L) return null
        buffer?.let { VideoNativeBindings.nDrawVideo(nativePtr, it) }
        return buffer
    }

    fun play() {
        if (nativePtr != 0L) VideoNativeBindings.nPlayVideo(nativePtr)
    }

    fun pause() {
        if (nativePtr != 0L) VideoNativeBindings.nPauseVideo(nativePtr)
    }

    fun stop() {
        if (nativePtr != 0L) VideoNativeBindings.nStopVideo(nativePtr)
    }

    fun seekTo(timeMillis: Long) {
        if (nativePtr != 0L) VideoNativeBindings.nSeekVideo(nativePtr, timeMillis)
    }

    fun loop(loop: Boolean) {
        if (nativePtr != 0L) VideoNativeBindings.nLoopVideo(nativePtr, loop)
    }

    fun volume(volume: Float) {
        if (nativePtr != 0L) VideoNativeBindings.nSetVideoVolume(nativePtr, volume)
    }

    fun mute(muted: Boolean) {
        if (nativePtr != 0L) VideoNativeBindings.nSetVideoMuted(nativePtr, muted)
    }

    fun destroy() {
        val ptr = nativePtr
        if (ptr == 0L) return

        buffer = null
        nativePtr = 0L
        VideoNativeBindings.nDestroyVideo(ptr)
    }

    companion object {
        private const val VIDEO_INFO_WIDTH = 0
        private const val VIDEO_INFO_HEIGHT = 1
        private const val VIDEO_INFO_DURATION = 2
        private const val VIDEO_INFO_COUNT = 3
    }
}
