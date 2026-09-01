package org.example.myapp.auth.model

import org.example.myapp.auth.network.MediaType

data class PickedMedia(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray
) {
    val mediaType: MediaType
        get() = if (mimeType.startsWith("video/", ignoreCase = true)) {
            MediaType.VIDEO
        } else {
            MediaType.IMAGE
        }
}