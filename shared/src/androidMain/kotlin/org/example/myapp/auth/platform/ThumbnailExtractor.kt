package org.example.myapp.auth.platform

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

actual class ThumbnailExtractor actual constructor() {
    actual suspend fun extractThumbnail(videoBytes: ByteArray): ByteArray? =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            var tempFile: File? = null

            try {
                tempFile = File.createTempFile("temp_vid_", ".mp4")
                FileOutputStream(tempFile).use { it.write(videoBytes) }
                retriever.setDataSource(tempFile.absolutePath)

                val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                bitmap?.let {
                    val stream = ByteArrayOutputStream()
                    it.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    stream.toByteArray()
                }
            } catch (e: Exception) {
                null
            } finally {
                retriever.release()
                tempFile?.delete()
            }
        }
}