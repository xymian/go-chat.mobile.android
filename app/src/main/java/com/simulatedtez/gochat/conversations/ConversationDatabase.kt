package com.simulatedtez.gochat.conversations

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.conversations.models.Conversation
import com.simulatedtez.gochat.database.AppDatabase

class ConversationDatabase private constructor(private val conversationsDao: ConversationDao) {

    companion object {
        private var instance: ConversationDatabase? = null
        fun get(context: Context): ConversationDatabase {
            return instance ?: synchronized(this) {
                ConversationDatabase(
                    AppDatabase.getInstance(context).conversationsDao()
                )
            }
        }
    }

    suspend fun getConversations(): List<Conversation_db> {
        return conversationsDao.getAll()
    }

    suspend fun insertConversation(convo: Conversation_db) {
        conversationsDao.insert(convo)
    }
}

@Entity(tableName = "conversations")
data class Conversation_db(
    @PrimaryKey
    @ColumnInfo("chatReference")
    val chatReference: String,
    @ColumnInfo("otherUser")
    val otherUser: String,
    @ColumnInfo("lastMessage")
    val lastMessage: String = "",
    @ColumnInfo("timestamp")
    var timestamp: String = "",
    @ColumnInfo("unreadCount")
    var unreadCount: Int = 0,
    @ColumnInfo("contactAvi")
    val contactAvi: String =  "",
)

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE chatReference = :chatRef")
    suspend fun getByChatReference(chatRef: String): Conversation_db

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conv: Conversation_db)

    @Query("SELECT * FROM conversations ORDER BY timestamp DESC")
    suspend fun getAll(): List<Conversation_db>

    @Query("DELETE FROM conversations WHERE chatReference = :chatRef")
    suspend fun deleteByChatReference(chatRef: String)

    @Query("DELETE FROM conversations")
    suspend fun deleteAll()
}

fun Conversation_db.toConversation(): Conversation {
    return Conversation(
        me = session.username,
        other = otherUser,
        chatReference = chatReference,
        lastMessage = lastMessage,
        timestamp = timestamp,
        unreadCount = unreadCount,
        contactAvi = contactAvi
    )
}

fun List<Conversation_db>.toConversations(): List<Conversation> {
    return map {
        it.toConversation()
    }
}