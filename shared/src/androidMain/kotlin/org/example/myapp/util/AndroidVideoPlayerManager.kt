package org.example.myapp.util

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.myapp.manager.VideoPlayerViewManager

class AndroidVideoPlayerManager(context: Context): ViewModel(), VideoPlayerViewManager {
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build().apply {
        repeatMode = ExoPlayer.REPEAT_MODE_OFF
        volume = 0f
    }

    private val _currentPlayingUrl = MutableStateFlow<String?>(null)
    override val currentPlayingUrl: StateFlow<String?> = _currentPlayingUrl.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isEnded = MutableStateFlow(false)
    override val isEnded: StateFlow<Boolean> = _isEnded.asStateFlow()

    private val _isMuted = MutableStateFlow(true)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isPlayerReady = MutableStateFlow(false)
    val isPlayerReady: StateFlow<Boolean> = _isPlayerReady.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                _isEnded.value = (state == Player.STATE_ENDED)
                if (state == Player.STATE_READY) {
                    _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
                    _isPlayerReady.value = true
                }
            }
        })
    }

    override fun play(url: String) {
        if (_currentPlayingUrl.value == url) {
            if (_isEnded.value) {
                exoPlayer.seekTo(0)
            }
            exoPlayer.play()
            return
        }
        _currentPlayingUrl.value = url
        _isPlayerReady.value = false
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    override fun pause() {
        exoPlayer.pause()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }

    fun toggleMute() {
        val nextMuted = !_isMuted.value
        _isMuted.value = nextMuted
        exoPlayer.volume = if (nextMuted) 0f else 1f
    }

    override fun release() {
        exoPlayer.release()
    }

}