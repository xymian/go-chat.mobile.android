package com.simulatedtez.gochat.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.simulatedtez.gochat.chat.database.MessagesDao

abstract class AppDatabase: RoomDatabase() {
    abstract fun messagesDao(): MessagesDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chat_database"
                ).build().also {
                    instance = it
                }
            }
        }
    }
}