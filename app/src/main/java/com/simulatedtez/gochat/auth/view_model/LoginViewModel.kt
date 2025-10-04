package com.simulatedtez.gochat.auth.view_model

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.auth.remote.api_services.AuthApiService
import com.simulatedtez.gochat.auth.remote.api_usecases.LoginUsecase
import com.simulatedtez.gochat.auth.remote.models.LoginResponse
import com.simulatedtez.gochat.auth.repository.LoginEventListener
import com.simulatedtez.gochat.auth.repository.LoginRepository
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.client
import com.simulatedtez.gochat.utils.AppWideChatEventListener
import com.simulatedtez.gochat.utils.CleanupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginRepository: LoginRepository,
): ViewModel(), LoginEventListener {

    private val _isLoginSuccessful = MutableLiveData<Boolean?>()
    val isLoginSuccessful: LiveData<Boolean?> = _isLoginSuccessful

    private val _isLoggingIn = MutableLiveData<Boolean>()
    val isLoggingIn: LiveData<Boolean> = _isLoggingIn

    fun login(username: String, password: String) {
        _isLoggingIn.value = true
        viewModelScope.launch(Dispatchers.IO) {
            loginRepository.login(username, password)
        }
    }

    override fun onLoginFailed(errorResponse: IResponse.Failure<ParentResponse<LoginResponse>>) {
        _isLoggingIn.value = false
        _isLoginSuccessful.value = false
    }

    override fun onLogin(loginInfo: LoginResponse) {
        _isLoginSuccessful.value = true
        _isLoggingIn.value = false
    }

    fun resetLoginState() {
        _isLoginSuccessful.postValue(null)
    }

    fun cancel() {
        viewModelScope.cancel()
    }

    fun initializeAppWideChatService(context: Context) {
        session.setupAppWideChatService(AppWideChatEventListener.get(context))
    }
}

class LoginViewModelFactory(private val context: Context): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = LoginRepository(
            LoginUsecase(AuthApiService(client)),
            CleanupManager.get(context)
        )
        return LoginViewModel(repo).apply {
            repo.setEventListener(this)
        } as T
    }
}