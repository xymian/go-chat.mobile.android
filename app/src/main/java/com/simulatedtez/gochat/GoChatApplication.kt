package com.simulatedtez.gochat

import android.app.Application
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.utils.INetworkMonitor
import com.simulatedtez.gochat.utils.NetworkMonitor

class GoChatApplication: Application(), INetworkMonitor {

    private lateinit var networkMonitor: NetworkMonitor

    override fun onCreate() {
        super.onCreate()
        UserPreference.init(applicationContext)
        UserPreference.getUsername()?.let {
            session.saveUsername(it)
        }
        UserPreference.getAccessToken()?.let {
            session.saveAccessToken(it)
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