package com.simulatedtez.gochat.utils

interface INetworkMonitor {
    fun setCallback(callbacks: NetworkMonitor.Callbacks)
    fun removeCallback()
}