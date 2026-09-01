package org.example.myapp.auth.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.example.myapp.ImagePresignedRequest
import org.example.myapp.VideoPresignedRequest
import org.example.myapp.auth.local.SessionManager
import org.example.myapp.auth.model.PickedMedia
import org.example.myapp.auth.network.MediaApiService
import org.example.myapp.auth.network.MediaType
import org.example.myapp.auth.platform.ThumbnailExtractor
import kotlin.coroutines.cancellation.CancellationException

data class MediaUploadResult(
    val mediaUrl: String,
    val thumbnailUrl: String,
    val mediaType: MediaType
)

class MediaRepository(
    private val mediaApiService: MediaApiService,
    private val sessionManager: SessionManager,
    private val thumbnailExtractor: ThumbnailExtractor = ThumbnailExtractor()
) {
    private fun getAccessToken(): String =
        sessionManager.sessionFlow.value?.accessToken ?: throw IllegalStateException("로그인이 필요합니다.")

    suspend fun uploadMedia(media: PickedMedia): Result<MediaUploadResult> = withContext(Dispatchers.IO) {
        runCatching {
            when (media.mediaType) {
                MediaType.IMAGE -> {
                    val presigned = mediaApiService.getImagePresignedUrl(
                        token = getAccessToken(),
                        request = ImagePresignedRequest(
                            fileName = media.fileName,
                            contentType = media.mimeType
                        )
                    )
                    mediaApiService.uploadBinaryToR2(presigned.uploadUrl, media.bytes, media.mimeType)

                    MediaUploadResult(
                        mediaUrl = presigned.fileUrl,
                        thumbnailUrl = presigned.fileUrl,
                        mediaType = MediaType.IMAGE
                    )
                }
                MediaType.VIDEO -> {
                    val thumbBytes = thumbnailExtractor.extractThumbnail(media.bytes)
                        ?: throw IllegalStateException("동영상 썸네일 생성에 실패했습니다.")
                    val thumbFileName = "thumb_${media.fileName.substringBeforeLast(".")}.jpg"

                    val presigned = mediaApiService.getVideoPresignedUrl(
                        token = getAccessToken(),
                        request = VideoPresignedRequest(
                            videoFileName = media.fileName,
                            videoContentType = media.mimeType,
                            thumbFileName = thumbFileName,
                            thumbContentType = "image/jpeg"
                        )
                    )

                    mediaApiService.uploadBinaryToR2(presigned.video.uploadUrl, media.bytes, media.mimeType)
                    mediaApiService.uploadBinaryToR2(presigned.thumbnail.uploadUrl, thumbBytes, "image/jpeg")

                    MediaUploadResult(
                        mediaUrl = presigned.video.fileUrl,
                        thumbnailUrl = presigned.thumbnail.fileUrl,
                        mediaType = MediaType.VIDEO
                    )
                }
            }
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }
}