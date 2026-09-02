package org.example.myapp.util

import android.view.LayoutInflater
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import org.example.myapp.shared.R

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUrl: String,
    thumbnailUrl: String? = null,
    videoManager: AndroidVideoPlayerManager,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true
) {
    val currentUrl by videoManager.currentPlayingUrl.collectAsState()
    val isPlayingState by videoManager.isPlaying.collectAsState()
    val isEndedState by videoManager.isEnded.collectAsState()
    val isMuted by videoManager.isMuted.collectAsState()
    val isPlayerReady by videoManager.isPlayerReady.collectAsState()
    val totalDurationMs by videoManager.durationMs.collectAsState()

    val isCurrentCard = (currentUrl == videoUrl)
    val isPlaying = isCurrentCard && isPlayingState
    val isEnded = isCurrentCard && isEndedState

    var currentTimeMs by remember { mutableLongStateOf(0L) }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(videoUrl) {
        if (autoPlay && currentUrl == null) {
            videoManager.play(videoUrl)
        }
    }

    LaunchedEffect(isPlaying, isEnded) {
        while (isPlaying && !isEnded) {
            if (!isDragging) {
                currentTimeMs = videoManager.exoPlayer.currentPosition.coerceAtLeast(0L)
            }
            delay(30)
        }
    }

    val handlePlayPause = {
        if (isCurrentCard) {
            if (isEnded) {
                videoManager.seekTo(0)
                videoManager.play(videoUrl)
            } else if (isPlaying) {
                videoManager.pause()
            } else {
                videoManager.play(videoUrl)
            }
        } else {
            videoManager.play(videoUrl)
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
        if (isCurrentCard) {
            AndroidView(
                factory = { ctx ->
                    val view = LayoutInflater.from(ctx)
                        .inflate(R.layout.view_video_player, null, false) as PlayerView
                    view.apply {
                        player = videoManager.exoPlayer
                        setKeepContentOnPlayerReset(true)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        // 2. 영상 재생 전 썸네일 화면
        if ((!isCurrentCard || !isPlayerReady) && !thumbnailUrl.isNullOrEmpty()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3. 일시정지 / 다시재생 중앙 아이콘
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

        // 4. 우측 상단 음소거 버튼
        IconButton(
            onClick = { videoManager.toggleMute() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(24.dp)
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

        // 5. 영상 최하단 터치 영역
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
                .pointerInput(isCurrentCard, totalDurationMs) {
                    detectTapGestures { offset ->
                        if (isCurrentCard && totalDurationMs > 0) {
                            val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                            val targetMs = (ratio * totalDurationMs).toLong()
                            videoManager.seekTo(targetMs)
                            currentTimeMs = targetMs
                        }
                    }
                }
                .pointerInput(isCurrentCard, totalDurationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            if (isCurrentCard) {
                                isDragging = true
                                dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                            }
                        },
                        onDragEnd = {
                            if (isCurrentCard) {
                                val targetMs = (dragProgress * totalDurationMs).toLong()
                                videoManager.seekTo(targetMs)
                                isDragging = false
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { change, _ ->
                            if (isCurrentCard) {
                                change.consume()
                                val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                                dragProgress = ratio
                                currentTimeMs = (ratio * totalDurationMs).toLong()
                            }
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