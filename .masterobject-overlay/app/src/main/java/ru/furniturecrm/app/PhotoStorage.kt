package ru.furniturecrm.app

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

object PhotoStorage {
    data class SavedPhoto(val uri: String, val name: String, val mimeType: String)

    private fun directory(context: Context, projectId: Long): File =
        File(context.filesDir, "project_photos/$projectId").apply { mkdirs() }

    fun copyIntoApp(context: Context, source: Uri, projectId: Long, sourceName: String, mimeType: String?): SavedPhoto {
        val ext = extensionFrom(sourceName, mimeType)
        val safeName = "photo_${timestamp()}_${UUID.randomUUID().toString().take(8)}.$ext"
        val target = File(directory(context, projectId), safeName)
        context.contentResolver.openInputStream(source).use { input ->
            requireNotNull(input) { "Не удалось открыть выбранное фото" }
            target.outputStream().use { output -> input.copyTo(output) }
        }
        return SavedPhoto(Uri.fromFile(target).toString(), safeName, mimeType ?: mimeFromExtension(ext))
    }

    fun newCameraTarget(context: Context, projectId: Long): Pair<File, Uri> {
        val file = File(directory(context, projectId), "camera_${timestamp()}_${UUID.randomUUID().toString().take(8)}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return file to uri
    }

    fun savedCameraPhoto(file: File): SavedPhoto =
        SavedPhoto(Uri.fromFile(file).toString(), file.name, "image/jpeg")

    fun deleteOwnedPhoto(uriString: String) {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return
        if (uri.scheme == "file") runCatching { File(requireNotNull(uri.path)).delete() }
    }

    private fun timestamp(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))

    private fun extensionFrom(name: String, mime: String?): String {
        val fromName = name.substringAfterLast('.', "").lowercase().takeIf { it in setOf("jpg", "jpeg", "png", "webp", "heic", "heif") }
        if (fromName != null) return if (fromName == "jpeg") "jpg" else fromName
        return when (mime?.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/heic", "image/heif" -> "heic"
            else -> "jpg"
        }
    }

    private fun mimeFromExtension(ext: String): String = when (ext) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "heic" -> "image/heic"
        else -> "image/jpeg"
    }
}
