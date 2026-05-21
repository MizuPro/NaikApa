package com.example.naikapa.data.local

import android.content.Context
import com.example.naikapa.common.AppConstants
import java.io.File
import java.io.IOException

object PrebuiltDatabaseCopier {
    const val ASSET_DATABASE_PATH = "databases/naikapa_gtfs.db"

    fun ensureGtfsDatabaseCopied(context: Context): String {
        val databaseFile = context.getDatabasePath(AppConstants.DATABASE_NAME)
        if (databaseFile.exists()) return AppConstants.DATABASE_NAME

        try {
            copyAssetDatabase(context, databaseFile)
        } catch (_: IOException) {
            // Keep the app usable if the asset is absent in a test/build variant.
            databaseFile.parentFile?.mkdirs()
        }

        return AppConstants.DATABASE_NAME
    }

    private fun copyAssetDatabase(context: Context, databaseFile: File) {
        databaseFile.parentFile?.mkdirs()
        context.assets.open(ASSET_DATABASE_PATH).use { input ->
            databaseFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}
