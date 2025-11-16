package com.simulatedtez.gochat

import android.app.Application
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.util.AppWideChatEventListener
import com.simulatedtez.gochat.util.INetworkMonitor
import com.simulatedtez.gochat.util.NetworkMonitor
import java.time.LocalDateTime

class GoChatApplication: Application(), INetworkMonitor {

    private lateinit var networkMonitor: NetworkMonitor

    override fun onCreate() {
        super.onCreate()
        UserPreference.init(applicationContext)
        session.setupAppWideChatService(AppWideChatEventListener.get(applicationContext))

        if (UserPreference.getCutOffDateForMarkingMessagesAsSeen() == null) {
            UserPreference.storeCutOffDateForMarkingMessagesAsSeen(
                LocalDateTime.of(
                    2025, 8, 23, 0, 0
                )
            )
        }
        networkMonitor = NetworkMonitor(this.applicationContext)
        networkMonitor.start()
    }

    override fun setCallback(callbacks: NetworkMonitor.Callbacks) {
        networkMonitor.setCallback(callbacks)
    }

    override fun removeCallback() {
        networkMonitor.removeCallback()
    }
}