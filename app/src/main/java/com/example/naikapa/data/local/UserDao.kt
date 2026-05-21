package com.example.naikapa.data.local

import android.content.ContentValues
import android.database.Cursor
import com.example.naikapa.data.model.User
import com.example.naikapa.data.model.UserProfile

class UserDao(private val dbHelper: NaikApaDatabaseHelper) {
    fun insertUser(user: User): Long {
        val values = ContentValues().apply {
            put(NaikApaDbContract.Users.NAMA, user.nama)
            put(NaikApaDbContract.Users.EMAIL, user.email)
            put(NaikApaDbContract.Users.PASSWORD, user.password)
            put(NaikApaDbContract.Users.HAS_MOTOR, user.hasMotor.toInt())
            put(NaikApaDbContract.Users.HAS_CAR, user.hasCar.toInt())
            put(NaikApaDbContract.Users.CREATED_AT, user.createdAt)
        }
        return dbHelper.writableDatabase.insert(NaikApaDbContract.Users.TABLE, null, values)
    }

    fun isEmailExists(email: String): Boolean {
        dbHelper.readableDatabase.query(
            NaikApaDbContract.Users.TABLE,
            arrayOf(NaikApaDbContract.Users.ID),
            "${NaikApaDbContract.Users.EMAIL} = ?",
            arrayOf(email),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    fun login(email: String, password: String): User? {
        return getUser(
            selection = "${NaikApaDbContract.Users.EMAIL} = ? AND ${NaikApaDbContract.Users.PASSWORD} = ?",
            selectionArgs = arrayOf(email, password)
        )
    }

    fun getUserById(idUser: Long): User? {
        return getUser(
            selection = "${NaikApaDbContract.Users.ID} = ?",
            selectionArgs = arrayOf(idUser.toString())
        )
    }

    fun updateUser(user: User): Int {
        val values = ContentValues().apply {
            put(NaikApaDbContract.Users.NAMA, user.nama)
            put(NaikApaDbContract.Users.EMAIL, user.email)
            put(NaikApaDbContract.Users.PASSWORD, user.password)
            put(NaikApaDbContract.Users.HAS_MOTOR, user.hasMotor.toInt())
            put(NaikApaDbContract.Users.HAS_CAR, user.hasCar.toInt())
        }
        return dbHelper.writableDatabase.update(
            NaikApaDbContract.Users.TABLE,
            values,
            "${NaikApaDbContract.Users.ID} = ?",
            arrayOf(user.idUser.toString())
        )
    }

    fun deleteUser(idUser: Long): Int {
        return dbHelper.writableDatabase.delete(
            NaikApaDbContract.Users.TABLE,
            "${NaikApaDbContract.Users.ID} = ?",
            arrayOf(idUser.toString())
        )
    }

    fun upsertProfile(profile: UserProfile): Long {
        val values = ContentValues().apply {
            put(NaikApaDbContract.UserProfiles.ID_USER, profile.idUser)
            put(NaikApaDbContract.UserProfiles.DEFAULT_MODE, profile.defaultMode)
            put(NaikApaDbContract.UserProfiles.DEFAULT_PRIORITY, profile.defaultPriority)
            putNullableDouble(NaikApaDbContract.UserProfiles.HOME_LAT, profile.homeLat)
            putNullableDouble(NaikApaDbContract.UserProfiles.HOME_LON, profile.homeLon)
            put(NaikApaDbContract.UserProfiles.HOME_LABEL, profile.homeLabel)
        }
        val updatedRows = dbHelper.writableDatabase.update(
            NaikApaDbContract.UserProfiles.TABLE,
            values,
            "${NaikApaDbContract.UserProfiles.ID_USER} = ?",
            arrayOf(profile.idUser.toString())
        )
        return if (updatedRows > 0) updatedRows.toLong() else {
            dbHelper.writableDatabase.insert(NaikApaDbContract.UserProfiles.TABLE, null, values)
        }
    }

    fun getProfile(idUser: Long): UserProfile? {
        dbHelper.readableDatabase.query(
            NaikApaDbContract.UserProfiles.TABLE,
            null,
            "${NaikApaDbContract.UserProfiles.ID_USER} = ?",
            arrayOf(idUser.toString()),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toUserProfile() else null
        }
    }

    private fun getUser(selection: String, selectionArgs: Array<String>): User? {
        dbHelper.readableDatabase.query(
            NaikApaDbContract.Users.TABLE,
            null,
            selection,
            selectionArgs,
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toUser() else null
        }
    }

    private fun Cursor.toUser(): User {
        return User(
            idUser = getLong(getColumnIndexOrThrow(NaikApaDbContract.Users.ID)),
            nama = getString(getColumnIndexOrThrow(NaikApaDbContract.Users.NAMA)),
            email = getString(getColumnIndexOrThrow(NaikApaDbContract.Users.EMAIL)),
            password = getString(getColumnIndexOrThrow(NaikApaDbContract.Users.PASSWORD)),
            hasMotor = getInt(getColumnIndexOrThrow(NaikApaDbContract.Users.HAS_MOTOR)) == 1,
            hasCar = getInt(getColumnIndexOrThrow(NaikApaDbContract.Users.HAS_CAR)) == 1,
            createdAt = getLong(getColumnIndexOrThrow(NaikApaDbContract.Users.CREATED_AT))
        )
    }

    private fun Cursor.toUserProfile(): UserProfile {
        return UserProfile(
            idProfile = getLong(getColumnIndexOrThrow(NaikApaDbContract.UserProfiles.ID)),
            idUser = getLong(getColumnIndexOrThrow(NaikApaDbContract.UserProfiles.ID_USER)),
            defaultMode = getNullableString(NaikApaDbContract.UserProfiles.DEFAULT_MODE),
            defaultPriority = getNullableString(NaikApaDbContract.UserProfiles.DEFAULT_PRIORITY),
            homeLat = getNullableDouble(NaikApaDbContract.UserProfiles.HOME_LAT),
            homeLon = getNullableDouble(NaikApaDbContract.UserProfiles.HOME_LON),
            homeLabel = getNullableString(NaikApaDbContract.UserProfiles.HOME_LABEL)
        )
    }
}

private fun Boolean.toInt(): Int = if (this) 1 else 0

private fun ContentValues.putNullableDouble(key: String, value: Double?) {
    if (value == null) putNull(key) else put(key, value)
}

private fun Cursor.getNullableString(columnName: String): String? {
    val index = getColumnIndexOrThrow(columnName)
    return if (isNull(index)) null else getString(index)
}

private fun Cursor.getNullableDouble(columnName: String): Double? {
    val index = getColumnIndexOrThrow(columnName)
    return if (isNull(index)) null else getDouble(index)
}
