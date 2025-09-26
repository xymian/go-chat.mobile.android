package com.simulatedtez.gochat.chat.models

enum class PresenceStatus {
    ONLINE, AWAY, OFFLINE;

    companion object {
        fun getType(value: String?): PresenceStatus? {
            return when (value) {
                ONLINE.name -> ONLINE
                AWAY.name -> AWAY
                OFFLINE.name -> OFFLINE
                else -> null
            }
        }
    }
}