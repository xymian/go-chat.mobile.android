package com.simulatedtez.gochat.auth.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.simulatedtez.gochat.auth.remote.api_services.AuthApiService
import com.simulatedtez.gochat.auth.remote.api_usecases.SignupUsecase
import com.simulatedtez.gochat.auth.remote.models.SignupResponse
import com.simulatedtez.gochat.auth.repository.SignupEventListener
import com.simulatedtez.gochat.auth.repository.SignupRepository
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SignupViewModel(
    private val signupRepository: SignupRepository,
): ViewModel(), SignupEventListener {

    private val _isSignupSuccessful = MutableLiveData<Boolean>()
    val isSignupSuccessful: LiveData<Boolean> = _isSignupSuccessful

    private val _isSigningUp = MutableLiveData<Boolean>()
    val isSigningUp: LiveData<Boolean> = _isSigningUp

    fun signUp(username: String, password: String) {
        _isSigningUp.value = true
        viewModelScope.launch(Dispatchers.IO) {
            signupRepository.signUp(username, password)
        }
    }

    override fun onSignUp() {
        _isSigningUp.value = false
        _isSignupSuccessful.value = true
    }

    override fun onSignUpFailed(errorResponse: IResponse.Failure<ParentResponse<String>>) {
        _isSigningUp.value = false
        _isSignupSuccessful.value = false
    }

    fun cancel() {
        viewModelScope.cancel()
    }
}

class SignupViewModelProvider(): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = SignupRepository(SignupUsecase(AuthApiService(client)))
        return SignupViewModel(repo).apply {
            repo.setEventListener(this)
        } as T
    }
}