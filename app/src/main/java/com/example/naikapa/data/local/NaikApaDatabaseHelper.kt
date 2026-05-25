package com.example.naikapa.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.naikapa.common.AppConstants

class NaikApaDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    PrebuiltDatabaseCopier.ensureGtfsDatabaseCopied(context.applicationContext),
    null,
    AppConstants.DATABASE_VERSION
) {
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        NaikApaDbContract.createTableStatements.forEach(db::execSQL)
        NaikApaDbContract.indexStatements.forEach(db::execSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS ${NaikApaDbContract.RouteCache.TABLE}")
            val createRouteCache = NaikApaDbContract.createTableStatements.first {
                it.contains("CREATE TABLE ${NaikApaDbContract.RouteCache.TABLE}")
            }
            db.execSQL(createRouteCache)
            NaikApaDbContract.indexStatements
                .filter { it.contains(NaikApaDbContract.RouteCache.TABLE) }
                .forEach(db::execSQL)
            return
        }
        NaikApaDbContract.dropTableStatements.forEach(db::execSQL)
        onCreate(db)
    }
}
