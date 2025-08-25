package org.exthm.featuresettings.utils

import android.os.SystemProperties
import android.provider.Settings
import android.provider.Settings.Secure
import android.content.ContentResolver

object SystemPropertiesHelper {

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return SystemProperties.getBoolean(key, defaultValue)
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return SystemProperties.getInt(key, defaultValue)
    }

    fun set(key: String, value: String) {
        try {
            SystemProperties.set(key, value)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun get(key: String, def: String): String {
        return SystemProperties.get(key, def)
    }

    fun getSecureString(
        resolver: ContentResolver,
        key: String,
        defaultValue: String
    ): String {
        return Secure.getString(resolver, key) ?: defaultValue
    }

    fun setSecureString(
        resolver: ContentResolver,
        key: String,
        value: String
    ) {
        Secure.putString(resolver, key, value)
    }

}