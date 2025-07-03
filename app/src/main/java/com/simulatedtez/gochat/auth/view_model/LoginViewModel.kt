package com.simulatedtez.gochat.auth.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simulatedtez.gochat.auth.remote.models.LoginResponse
import com.simulatedtez.gochat.auth.repository.LoginEventListener
import com.simulatedtez.gochat.auth.repository.LoginRepository
import com.simulatedtez.gochat.remote.IResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginRepository: LoginRepository,
): ViewModel(), LoginEventListener {

    private val _isLoginSuccessful = MutableLiveData<Boolean>()
    val isLoginSuccessful: LiveData<Boolean> = _isLoginSuccessful

    private val _isLoggingIn = MutableLiveData<Boolean>()
    val isLoggingIn: LiveData<Boolean> = _isLoggingIn

    fun login(username: String, password: String) {
        _isLoggingIn.value = true
        viewModelScope.launch(Dispatchers.IO) {
            loginRepository.login(username, password)
        }
    }

    override fun onLoginFailed(errorResponse: IResponse.Failure<LoginResponse>) {
        _isLoggingIn.value = false
    }

    override fun onLogin(loginInfo: LoginResponse) {
        _isLoggingIn.value = false
    }

    fun cancel() {
        viewModelScope.cancel()
    }
}