package com.simulatedtez.gochat

import android.app.Application
import com.simulatedtez.gochat.utils.INetworkMonitor
import com.simulatedtez.gochat.utils.NetworkMonitor

class GoChatApplication: Application(), INetworkMonitor {

    private lateinit var networkMonitor: NetworkMonitor

    override fun onCreate() {
        super.onCreate()
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