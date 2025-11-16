package com.simulatedtez.gochat.util

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.content.Context
import android.net.NetworkCapabilities

class NetworkMonitor(context: Context) {

    private var callbacks: NetworkMonitor.Callbacks? = null

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            callbacks?.onAvailable()
        }

        override fun onLost(network: Network) {
            callbacks?.onLost()
        }
    }

    fun start() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    fun stop() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    fun setCallback(callbacks: Callbacks) {
        this.callbacks = callbacks
    }

    fun removeCallback() {
        callbacks = null
    }

    interface Callbacks {
        fun onAvailable()
        fun onLost()
    }
}
