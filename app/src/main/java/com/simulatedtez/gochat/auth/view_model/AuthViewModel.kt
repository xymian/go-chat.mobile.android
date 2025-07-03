package com.simulatedtez.gochat.auth.view_model

import androidx.lifecycle.ViewModel
import com.simulatedtez.gochat.auth.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AuthViewModel(private val authRepository: AuthRepository): ViewModel() {

    private val context = CoroutineScope(Dispatchers.Main + SupervisorJob())
}