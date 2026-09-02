package org.example.myapp.manager

import kotlinx.coroutines.flow.StateFlow

interface VideoPlayerViewManager {
    val currentPlayingUrl: StateFlow<String?>
    val isPlaying: StateFlow<Boolean>
    val isEnded: StateFlow<Boolean>

    fun play(url: String)
    fun pause()
    fun release()
}