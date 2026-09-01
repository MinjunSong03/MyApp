package org.example.myapp

import kotlinx.serialization.Serializable

@Serializable
data class ImagePresignedRequest(
    val fileName: String,
    val contentType: String
)

@Serializable
data class VideoPresignedRequest(
    val videoFileName: String,
    val videoContentType: String,
    val thumbFileName: String,
    val thumbContentType: String
)

@Serializable
data class PresignedUrlResponse(
    val uploadUrl: String,
    val fileUrl: String,
    val key: String
)

@Serializable
data class VideoPresignedUrlResponse(
    val video: PresignedUrlResponse,
    val thumbnail: PresignedUrlResponse
)