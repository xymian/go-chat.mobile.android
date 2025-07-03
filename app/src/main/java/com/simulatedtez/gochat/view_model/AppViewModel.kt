package com.simulatedtez.gochat.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

open class AppViewModel: ViewModel() {

    private val _isProcessing = MutableLiveData<Boolean>()
    private val isProcessing: LiveData<Boolean> = _isProcessing


    fun setProcessingStatus(status: Boolean) {
        _isProcessing.value = status
    }
}