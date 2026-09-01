package org.example.myapp.auth.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVAsset
import platform.AVFoundation.AVAssetImageGenerator
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy

actual class ThumbnailExtractor actual constructor() {
    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun extractThumbnail(videoBytes: ByteArray): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val tempFilePath = NSTemporaryDirectory() + "temp_${NSUUID.UUID().UUIDString}.mp4"
            val nsData = videoBytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = videoBytes.size.toULong())
            }
            nsData.writeToFile(tempFilePath, atomically = true)

            val fileUrl = NSURL.fileURLWithPath(tempFilePath)
            val asset = AVAsset.assetWithURL(fileUrl)
            val generator = AVAssetImageGenerator.assetImageGeneratorWithAsset(asset).apply {
                appliesPreferredTrackTransform = true
            }

            val time = CMTimeMake(value = 0, timescale = 1)
            val cgImage = generator.copyCGImageAtTime(time, actualTime = null, error = null) ?: return@withContext null
            val uiImage = UIImage.imageWithCGImage(cgImage)
            val jpegData = UIImageJPEGRepresentation(uiImage, 0.85) ?: return@withContext null

            val resultBytes = ByteArray(jpegData.length.toInt())
            resultBytes.usePinned { pinned ->
                memcpy(pinned.addressOf(0), jpegData.bytes, jpegData.length)
            }
            resultBytes
        } catch (e: Exception) {
            null
        }
    }
}