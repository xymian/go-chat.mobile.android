package com.simulatedtez.gochat.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.simulatedtez.gochat.chat.database.DBMessage
import com.simulatedtez.gochat.chat.database.MessagesDao
import com.simulatedtez.gochat.conversations.ConversationDao
import com.simulatedtez.gochat.conversations.DBConversation

@Database(
    entities = [DBMessage::class, DBConversation::class],
    version = 1
)
abstract class AppDatabase: RoomDatabase() {
    abstract fun messagesDao(): MessagesDao
    abstract fun conversationsDao(): ConversationDao

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