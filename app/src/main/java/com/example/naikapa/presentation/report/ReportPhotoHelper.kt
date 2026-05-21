package com.example.naikapa.presentation.report

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.naikapa.common.AppConstants
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper untuk membuat file foto laporan gangguan, mendapatkan URI FileProvider,
 * dan menormalisasi path foto yang akan disimpan ke SQLite.
 */
object ReportPhotoHelper {

    private const val AUTHORITY_SUFFIX = ".fileprovider"

    /**
     * Buat file kosong untuk menyimpan hasil foto kamera.
     * File disimpan di cache dir / disruption_photos/.
     */
    fun createPhotoFile(context: Context): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${AppConstants.DISRUPTION_PHOTO_PREFIX}${timestamp}${AppConstants.DISRUPTION_PHOTO_EXTENSION}"
        val photoDir = File(context.cacheDir, AppConstants.DISRUPTION_PHOTO_DIR).also { it.mkdirs() }
        return File(photoDir, fileName)
    }

    /**
     * Dapatkan URI FileProvider untuk file foto agar bisa dikirim ke intent kamera.
     */
    fun getUriForFile(context: Context, file: File): Uri {
        val authority = "${context.packageName}$AUTHORITY_SUFFIX"
        return FileProvider.getUriForFile(context, authority, file)
    }

    /**
     * Normalisasi path foto untuk disimpan ke SQLite.
     * Mengembalikan absolute path string, atau null jika file tidak valid.
     */
    fun normalizePath(file: File?): String? {
        if (file == null || !file.exists() || file.length() == 0L) return null
        return file.absolutePath
    }

    /**
     * Hapus file foto dari storage lokal jika ada.
     */
    fun deletePhoto(photoPath: String?) {
        if (photoPath.isNullOrBlank()) return
        try {
            File(photoPath).delete()
        } catch (_: Exception) { /* abaikan error hapus file */ }
    }
}
