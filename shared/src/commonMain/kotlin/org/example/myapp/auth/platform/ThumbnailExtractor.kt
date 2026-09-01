package org.example.myapp.auth.platform

expect class ThumbnailExtractor() {
    suspend fun extractThumbnail(videoBytes: ByteArray): ByteArray?
}