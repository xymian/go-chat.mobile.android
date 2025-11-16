package com.simulatedtez.gochat.util

interface INetworkMonitor {
    fun setCallback(callbacks: NetworkMonitor.Callbacks)
    fun removeCallback()
}