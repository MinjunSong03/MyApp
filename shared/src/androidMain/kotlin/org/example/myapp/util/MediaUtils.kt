package org.example.myapp.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.example.myapp.auth.model.PickedMedia

fun Uri.toPickedMedia(context: Context): PickedMedia? {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(this) ?: "application/octet-stream"

    var fileName = "media_${System.currentTimeMillis()}"
    resolver.query(this, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) {
            fileName = cursor.getString(nameIndex)
        }
    }

    val bytes = resolver.openInputStream(this)?.use { it.readBytes() } ?: return null

    return PickedMedia(
        fileName = fileName,
        mimeType = mimeType,
        bytes = bytes
    )
}