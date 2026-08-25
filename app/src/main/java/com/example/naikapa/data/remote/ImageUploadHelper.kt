package com.example.naikapa.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

object ImageUploadHelper {

    /**
     * Upload local image file directly to Backblaze B2 via Presigned PUT URL.
     * @param uploadUrl Presigned PUT URL from backend.
     * @param file Local image file on Android device cache/storage.
     * @param contentType MIME type (e.g. image/jpeg, image/png, image/webp).
     * @return Boolean true if HTTP response code is 200/201/204 OK.
     */
    suspend fun uploadPhotoToB2(
        uploadUrl: String,
        file: File,
        contentType: String = "image/jpeg",
        okHttpClient: OkHttpClient = RemoteClient.okHttpClient
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.length() == 0L) {
                return@withContext false
            }

            val mediaType = contentType.toMediaTypeOrNull()
            val requestBody = file.asRequestBody(mediaType)

            val request = Request.Builder()
                .url(uploadUrl)
                .put(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
