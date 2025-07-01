package com.simulatedtez.gochat.chat.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.chat.remote.models.toMessage_db
import com.simulatedtez.gochat.chat.remote.models.toMessages_db
import com.simulatedtez.gochat.database.AppDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatDatabase private constructor(private val messagesDao: MessagesDao): IChatStorage {

    var pageSize: Int? = null
    private var timestampsOfLastMessageInPages = hashMapOf<Int, String>(
        1 to SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()
        ).format(Date())
    )

    companion object {
        private const val PAGE_SIZE = 50

        fun get(context: Context): ChatDatabase {
            return synchronized(this) {
                ChatDatabase(AppDatabase.getInstance(context).messagesDao())
            }
        }
    }
    override suspend fun loadMessages(page: Int): List<Message> {
        return timestampsOfLastMessageInPages[page]?.let { pageNumber ->
            messagesDao.getMessages(pageNumber, pageSize ?: PAGE_SIZE).toMessages().sortedBy {
                it.messageTimestamp
            }.also {
                if (it.isNotEmpty()) {
                    timestampsOfLastMessageInPages[page] = it.first().messageTimestamp!!
                }
            }
        } ?: throw Exception()
    }

    override suspend fun store(message: Message) {
        messagesDao.insertMessage(message.toMessage_db())
    }

    override suspend fun store(messages: List<Message>) {
        messagesDao.insertMessages(messages.toMessages_db())
    }

}

@Entity(tableName = "messages")
data class Message_db(
    @PrimaryKey
    val messageReference: String? = null,
    val textMessage: String? = null,
    val senderUsername: String? = null,
    val receiverUsername: String? = null,
    val messageTimestamp: String? = null,
    val chatReference: String? = null,
    val seenByReceiver: Boolean,
)

fun Message_db.toMessage(): Message {
    return Message(
        messageReference = messageReference,
        senderUsername = senderUsername,
        receiverUsername = receiverUsername,
        textMessage = textMessage,
        chatReference = chatReference,
        messageTimestamp = messageTimestamp,
        seenByReceiver = seenByReceiver
    )
}

fun List<Message_db>.toMessages(): List<Message> {
    return map {
        it.toMessage()
    }
}


@Dao
interface MessagesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message_db)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<Message_db>)

    @Query("SELECT * FROM messages WHERE " +
            "messageTimestamp < :startTimestamp ORDER BY messageTimestamp DESC LIMIT :size")
    suspend fun getMessages(startTimestamp: String, size: Int): List<Message_db>

    @Query("SELECT * FROM messages ORDER BY messageTimestamp DESC LIMIT 1")
    suspend fun getLastMessage(): Message_db
}