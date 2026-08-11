package org.thorvg.compose.video

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.isActive
import org.thorvg.core.video.VideoComposition

@Composable
fun rememberVideoState(
    isPlaying: Boolean = true,
    loop: Boolean = false,
    volume: Float = 1f,
    muted: Boolean = false
): VideoState {
    return remember {
        VideoState(
            isPlaying = isPlaying,
            loop = loop,
            volume = volume,
            muted = muted
        )
    }
}

@Stable
class VideoState internal constructor(
    isPlaying: Boolean,
    loop: Boolean,
    volume: Float,
    muted: Boolean
) {
    var isPlaying by mutableStateOf(isPlaying)
        private set

    var isRunning by mutableStateOf(false)
        internal set

    var currentTimeMillis by mutableLongStateOf(0L)
        internal set

    var durationMillis by mutableLongStateOf(0L)
        internal set

    var loop by mutableStateOf(loop)

    private var _volume by mutableFloatStateOf(volume.coerceIn(0f, 1f))
    var volume: Float
        get() = _volume
        set(value) {
            _volume = value.coerceIn(0f, 1f)
        }

    var muted by mutableStateOf(muted)

    internal var seekRequests by mutableIntStateOf(0)
        private set

    internal var seekTargetMillis = 0L
        private set

    internal var resetRequests by mutableIntStateOf(0)
        private set

    fun play() {
        if (!loop && durationMillis > 0L && currentTimeMillis >= durationMillis) {
            seekTo(0L)
        }
        isPlaying = true
    }

    fun pause() {
        isPlaying = false
    }

    fun stop() {
        resetRequests++
        currentTimeMillis = 0L
        isPlaying = false
    }

    fun seekTo(timeMillis: Long) {
        val duration = durationMillis.coerceAtLeast(0L)
        // Past-the-end targets are ignored; playback continues to its natural finish.
        if (duration > 0L && timeMillis >= duration) return
        val target = timeMillis.coerceIn(0L, duration)
        seekTargetMillis = target
        currentTimeMillis = target
        seekRequests++
    }

    fun seekBy(deltaMillis: Long) {
        seekTo(currentTimeMillis + deltaMillis)
    }

    fun setLoopEnabled(enabled: Boolean) {
        loop = enabled
    }

    fun setMutedEnabled(enabled: Boolean) {
        muted = enabled
    }

    fun setVolumeLevel(level: Float) {
        volume = level
    }
}

@Composable
fun rememberVideoComposition(path: String): VideoComposition {
    val composition = remember(path) {
        VideoComposition(path)
    }

    DisposableEffect(composition) {
        onDispose {
            composition.release()
        }
    }

    return composition
}

@Composable
fun Video(
    path: String,
    modifier: Modifier = Modifier,
    state: VideoState = rememberVideoState()
) {
    val composition = rememberVideoComposition(path)
    VideoContent(
        composition = composition,
        modifier = modifier,
        state = state
    )
}

@Composable
fun Video(
    composition: VideoComposition,
    modifier: Modifier = Modifier,
    state: VideoState = rememberVideoState()
) {
    VideoContent(
        composition = composition,
        modifier = modifier,
        state = state
    )
}

@Composable
private fun VideoContent(
    composition: VideoComposition,
    modifier: Modifier = Modifier,
    state: VideoState = rememberVideoState()
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null, neverEqualPolicy()) }
    var renderedSize by remember { mutableStateOf(IntSize.Zero) }
    var consumedSeekRequest by remember(composition) { mutableIntStateOf(state.seekRequests) }
    var consumedResetRequest by remember(composition) { mutableIntStateOf(state.resetRequests) }

    LaunchedEffect(composition, state.loop, state.volume, state.muted) {
        if (composition.isValid()) {
            composition.loop(state.loop)
            composition.volume(state.volume)
            composition.mute(state.muted)
        }
    }

    LaunchedEffect(
        composition,
        canvasSize,
        state.isPlaying,
        state.seekRequests,
        state.resetRequests
    ) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0 || !composition.isValid()) {
            currentBitmap = null
            renderedSize = IntSize.Zero
            return@LaunchedEffect
        }

        val targetSize = if (composition.width > 0 && composition.height > 0) {
            val scale = minOf(
                canvasSize.width.toFloat() / composition.width,
                canvasSize.height.toFloat() / composition.height
            )
            IntSize(
                width = (composition.width * scale).toInt().coerceAtLeast(1),
                height = (composition.height * scale).toInt().coerceAtLeast(1)
            )
        } else {
            canvasSize
        }

        composition.setSize(targetSize.width, targetSize.height)
        state.durationMillis = composition.durationMillis
        renderedSize = targetSize

        val didReset = state.resetRequests != consumedResetRequest
        val didSeek = state.seekRequests != consumedSeekRequest
        consumedResetRequest = state.resetRequests
        consumedSeekRequest = state.seekRequests

        when {
            didReset -> {
                composition.stop()
            }
            didSeek -> {
                composition.seekTo(state.seekTargetMillis)
            }
        }

        composition.loop(state.loop)
        composition.volume(state.volume)
        composition.mute(state.muted)

        if (state.isPlaying) {
            composition.play()
        } else {
            composition.pause()
        }
        state.isRunning = state.isPlaying
        currentBitmap = composition.render()

        // Paused seek/stop: the target frame decodes asynchronously (up to a GOP behind a
        // keyframe), so keep rendering for a bounded time window instead of a frame count.
        val renderUntilNanos = if (!state.isPlaying && (didSeek || didReset)) {
            System.nanoTime() + SEEK_RENDER_WINDOW_NANOS
        } else 0L
        try {
            while (isActive) {
                withFrameNanos { }

                currentBitmap = composition.render()
                // A pending seek/reset holds the target in currentTimeMillis; overwriting it
                // with the stale native time before the restarted effect consumes the request
                // would lose the seek.
                if (state.seekRequests == consumedSeekRequest &&
                    state.resetRequests == consumedResetRequest
                ) {
                    state.currentTimeMillis = composition.currentTimeMillis
                }

                if (!state.isPlaying) {
                    if (System.nanoTime() >= renderUntilNanos) break
                } else if (!state.loop &&
                    state.durationMillis > 0L &&
                    state.currentTimeMillis >= state.durationMillis
                ) {
                    state.pause()
                    break
                }
            }
        } finally {
            state.isRunning = false
        }
    }

    Canvas(
        modifier = modifier.onSizeChanged { canvasSize = it }
    ) {
        currentBitmap?.let { bitmap ->
            val topLeft = Offset(
                x = ((size.width - renderedSize.width) / 2f).coerceAtLeast(0f),
                y = ((size.height - renderedSize.height) / 2f).coerceAtLeast(0f)
            )
            drawImage(bitmap.asImageBitmap(), topLeft = topLeft)
        }
    }
}

private const val SEEK_RENDER_WINDOW_NANOS = 2_500_000_000L
