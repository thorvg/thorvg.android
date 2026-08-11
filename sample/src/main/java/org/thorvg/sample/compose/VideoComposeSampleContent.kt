package org.thorvg.sample.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.thorvg.compose.video.Video
import org.thorvg.compose.video.rememberVideoComposition
import org.thorvg.compose.video.rememberVideoState
import org.thorvg.core.video.VideoComposition
import org.thorvg.sample.R
import kotlin.math.roundToLong

@Composable
fun VideoPathComposeSampleContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var videoPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(context) {
        videoPath = copySampleVideo(context)
    }

    videoPath?.let { path ->
        key(path) {
            VideoPlaybackContent(
                composition = rememberVideoComposition(path),
                modifier = modifier
            )
        }
    } ?: PreparingVideo(modifier)
}

@Composable
fun VideoDataComposeSampleContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var videoData by remember { mutableStateOf<ByteArray?>(null) }

    LaunchedEffect(context) {
        videoData = readSampleVideo(context)
    }

    videoData?.let { data ->
        key(data) {
            VideoPlaybackContent(
                composition = rememberVideoComposition(data, data.size),
                modifier = modifier
            )
        }
    } ?: PreparingVideo(modifier)
}

@Composable
private fun PreparingVideo(modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = "Preparing video…",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoPlaybackContent(
    composition: VideoComposition,
    modifier: Modifier = Modifier
) {
    val videoState = rememberVideoState(isPlaying = true, volume = 1f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val duration = videoState.durationMillis
        val progress = if (duration > 0L) {
            videoState.currentTimeMillis.toFloat() / duration.toFloat()
        } else {
            0f
        }
        var isSeeking by remember { mutableStateOf(false) }
        var seekProgress by remember { mutableFloatStateOf(0f) }
        val seekInteractionSource = remember { MutableInteractionSource() }
        val seekSliderColors = SliderDefaults.colors()
        val visibleProgress = if (isSeeking) seekProgress else progress.coerceIn(0f, 1f)
        val visibleTime = if (isSeeking) {
            (duration * visibleProgress).roundToLong()
        } else {
            videoState.currentTimeMillis
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Video(
                composition = composition,
                state = videoState,
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            text = "${formatTime(visibleTime)} / ${formatTime(duration)}",
            style = MaterialTheme.typography.bodyMedium
        )

        Slider(
            value = visibleProgress,
            onValueChange = { value ->
                isSeeking = true
                seekProgress = value.coerceIn(0f, 1f)
            },
            onValueChangeFinished = {
                val target = (duration * seekProgress).roundToLong()
                videoState.seekTo(target)
                isSeeking = false
            },
            enabled = duration > 0L,
            interactionSource = seekInteractionSource,
            colors = seekSliderColors,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = seekInteractionSource,
                    colors = seekSliderColors,
                    enabled = duration > 0L,
                    thumbSize = DpSize(width = 8.dp, height = 36.dp)
                )
            },
            track = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(visibleProgress)
                            .height(12.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(6.dp)
                            )
                    )
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            Button(
                onClick = { videoState.seekBy(-5_000L) },
                enabled = duration > 0L
            ) {
                Text("-5s")
            }

            Button(
                onClick = {
                    videoState.seekTo(0L)
                    videoState.play()
                },
                enabled = duration > 0L
            ) {
                Text("Replay")
            }

            Button(
                onClick = { videoState.seekBy(5_000L) },
                enabled = duration > 0L
            ) {
                Text("+5s")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            Button(onClick = {
                if (videoState.isPlaying) {
                    videoState.pause()
                } else {
                    videoState.play()
                }
            }) {
                Text(
                    if (videoState.isPlaying) {
                        stringResource(R.string.sample_pause)
                    } else {
                        stringResource(R.string.sample_resume)
                    }
                )
            }

            Button(onClick = { videoState.stop() }) {
                Text(stringResource(R.string.sample_stop))
            }

            Button(onClick = { videoState.setLoopEnabled(!videoState.loop) }) {
                Text(if (videoState.loop) "Loop: On" else "Loop: Off")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = { videoState.setMutedEnabled(!videoState.muted) }) {
                Text(if (videoState.muted) "Unmute" else "Mute")
            }

            Text(
                text = "Volume ${(videoState.volume * 100).roundToLong()}%",
                style = MaterialTheme.typography.bodyMedium
            )

            Slider(
                value = videoState.volume,
                onValueChange = { videoState.setVolumeLevel(it) },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            )
        }
    }
}

private fun formatTime(timeMillis: Long): String {
    val totalSeconds = (timeMillis / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}
