package org.example.myapp.util

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import org.example.myapp.shared.R

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    initialMuted: Boolean = true
) {
    val context = LocalContext.current
    var isMuted by remember { mutableStateOf(initialMuted) }
    var isPlaying by remember { mutableStateOf(autoPlay) }
    var isEnded by remember { mutableStateOf(false) }

    var currentTimeMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = autoPlay
            repeatMode = ExoPlayer.REPEAT_MODE_OFF
            volume = if (initialMuted) 0f else 1f

            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    isEnded = (playbackState == Player.STATE_ENDED)
                    if (playbackState == Player.STATE_READY) {
                        totalDurationMs = duration.coerceAtLeast(0L)
                    }
                }
            })
        }
    }

    LaunchedEffect(isPlaying, isEnded) {
        while (isPlaying && !isEnded) {
            if (!isDragging) {
                currentTimeMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
            }
            delay(50)
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    val handlePlayPause = {
        if (isEnded) {
            exoPlayer.seekTo(0)
            exoPlayer.play()
        } else if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    Box(
        modifier = modifier
            .background(Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                handlePlayPause()
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. 영상 재생 화면
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. 일시정지 / 다시재생 중앙 아이콘
        if (!isPlaying || isEnded) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(if (isEnded) R.drawable.ic_replay else R.drawable.ic_play),
                    contentDescription = if (isEnded) "다시 재생" else "재생",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // 3. 우측 상단 음소거 버튼
        IconButton(
            onClick = {
                isMuted = !isMuted
                exoPlayer.volume = if (isMuted) 0f else 1f
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(32.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                painter = if (isMuted) {
                    painterResource(R.drawable.ic_mute)
                } else {
                    painterResource(R.drawable.ic_sound)
                },
                contentDescription = if (isMuted) "음소거 해제" else "음소거",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        // 4. 영상 최하단 터치 영역
        val progress = if (isDragging) {
            dragProgress
        } else {
            if (totalDurationMs > 0) currentTimeMs.toFloat() / totalDurationMs else 0f
        }.coerceIn(0f, 1f)

        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(20.dp)
                .pointerInput(totalDurationMs) {
                    detectTapGestures { offset ->
                        if (totalDurationMs > 0) {
                            val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                            val targetMs = (ratio * totalDurationMs).toLong()
                            exoPlayer.seekTo(targetMs)
                            currentTimeMs = targetMs
                        }
                    }
                }
                .pointerInput(totalDurationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            val targetMs = (dragProgress * totalDurationMs).toLong()
                            exoPlayer.seekTo(targetMs)
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                            dragProgress = ratio
                            currentTimeMs = (ratio * totalDurationMs).toLong()
                        }
                    )
                },
            contentAlignment = Alignment.BottomStart
        ) {
            // 배경 실선
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.35f))
            )

            // 진행 실선
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress)
                    .height(2.dp)
                    .background(Color.Black)
            )
        }
    }
}