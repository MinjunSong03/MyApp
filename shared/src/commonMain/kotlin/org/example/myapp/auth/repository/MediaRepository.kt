package org.example.myapp.auth.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.example.myapp.ImagePresignedRequest
import org.example.myapp.VideoPresignedRequest
import org.example.myapp.auth.local.SessionManager
import org.example.myapp.auth.model.PickedMedia
import org.example.myapp.auth.network.MediaApiService
import org.example.myapp.auth.network.MediaType
import org.example.myapp.auth.platform.FastStartUtil
import org.example.myapp.auth.platform.ThumbnailExtractor
import kotlin.coroutines.cancellation.CancellationException

data class UploadedPostMediaResult(
    val videoUrl: String? = null,
    val videoThumbnailUrl: String? = null,
    val imageUrls: List<String> = emptyList()
)

class MediaRepository(
    private val mediaApiService: MediaApiService,
    private val sessionManager: SessionManager,
    private val thumbnailExtractor: ThumbnailExtractor = ThumbnailExtractor()
) {
    private fun getAccessToken(): String =
        sessionManager.sessionFlow.value?.accessToken ?: throw IllegalStateException("로그인이 필요합니다.")

    private suspend fun uploadSingleImage(image: PickedMedia, token: String): String {
        val presigned = mediaApiService.getImagePresignedUrl(
            token = token,
            request = ImagePresignedRequest(
                fileName = image.fileName,
                contentType = image.mimeType
            )
        )
        mediaApiService.uploadBinaryToR2(presigned.uploadUrl, image.bytes, image.mimeType)
        return presigned.fileUrl
    }

    suspend fun uploadPostMedia(
        video: PickedMedia?,
        images: List<PickedMedia>
    ): Result<UploadedPostMediaResult> = withContext(Dispatchers.IO) {
        runCatching {
            if (video == null && images.isEmpty()) {
                return@runCatching UploadedPostMediaResult()
            }

            val token = getAccessToken()

            coroutineScope {
                val imagesDeferred = async {
                    images.take(10).map { img ->
                        async { uploadSingleImage(img, token) }
                    }.awaitAll()
                }

                val videoDeferred = async {
                    if (video == null) return@async null

                    val thumbBytes = thumbnailExtractor.extractThumbnail(video.bytes)
                        ?: throw IllegalStateException("동영상 썸네일 생성에 실패했습니다.")
                    val thumbFileName = "thumb_${video.fileName.substringBeforeLast(".")}.jpg"

                    val presigned = mediaApiService.getVideoPresignedUrl(
                        token = token,
                        request = VideoPresignedRequest(
                            videoFileName = video.fileName,
                            videoContentType = video.mimeType,
                            thumbFileName = thumbFileName,
                            thumbContentType = "image/jpeg"
                        )
                    )

                    val fastStartBytes = FastStartUtil.process(video.bytes)
                    mediaApiService.uploadBinaryToR2(presigned.video.uploadUrl, fastStartBytes, video.mimeType)
                    mediaApiService.uploadBinaryToR2(presigned.thumbnail.uploadUrl, thumbBytes, "image/jpeg")

                    Pair(presigned.video.fileUrl, presigned.thumbnail.fileUrl)
                }

                val uploadedImageUrls = imagesDeferred.await()
                val uploadedVideoPair = videoDeferred.await()

                UploadedPostMediaResult(
                    videoUrl = uploadedVideoPair?.first,
                    videoThumbnailUrl = uploadedVideoPair?.second,
                    imageUrls = uploadedImageUrls
                )
            }
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }
}