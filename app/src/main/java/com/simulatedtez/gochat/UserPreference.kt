package com.simulatedtez.gochat

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object UserPreference {
    private const val NAME = "user_pref"
    private lateinit var preferences: SharedPreferences

    fun init(context: Context) {
        preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    }

    fun storeUsername(value: String) {
        preferences.edit { putString(USERNAME_PREF, value) }
    }

    fun getUsername(): String? {
        return preferences.getString(USERNAME_PREF, null)
    }

    fun storeAccessToken(value: String) {
        preferences.edit {
            putString(ACCESS_TOKEN_PREF, value)
        }
    }

    fun getAccessToken(): String? {
        return preferences.getString(ACCESS_TOKEN_PREF, null)
    }

    fun storeChatHistoryStatus(chatRef: String, isNew: Boolean) {
        preferences.edit {
            putBoolean(chatRef, isNew)
        }
    }

    fun isNewChatHistory(chatRef: String): Boolean {
        return preferences.getBoolean(chatRef, true)
    }
}

const val ACCESS_TOKEN_PREF = "access-token-pref"
const val USERNAME_PREF = "username-pref"