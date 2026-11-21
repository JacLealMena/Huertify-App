package com.duoc.huertify_app.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Guarda las evidencias fotográficas dentro del almacenamiento privado de la app. */
class ImageStorage(private val context: Context) {

    private val dir: File get() = File(context.filesDir, "evidence").apply { mkdirs() }

    /** Crea un archivo vacío y su Uri (FileProvider) para que la cámara escriba la foto. */
    fun newCameraUri(): Uri {
        val file = File(dir, "photo_${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** Copia una imagen elegida desde la galería a almacenamiento propio y devuelve su Uri. */
    suspend fun importFromGallery(source: Uri): Uri = withContext(Dispatchers.IO) {
        val file = File(dir, "gallery_${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(source).use { input ->
            requireNotNull(input) { "No se pudo leer la imagen" }
            file.outputStream().use { input.copyTo(it) }
        }
        Uri.fromFile(file)
    }
}
