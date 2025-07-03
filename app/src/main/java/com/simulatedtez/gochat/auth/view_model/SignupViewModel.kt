package com.simulatedtez.gochat.auth.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simulatedtez.gochat.auth.remote.models.SignupResponse
import com.simulatedtez.gochat.auth.repository.SignupEventListener
import com.simulatedtez.gochat.auth.repository.SignupRepository
import com.simulatedtez.gochat.remote.IResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SignupViewModel(
    private val signupRepository: SignupRepository,
    private val signupEventListener: SignupEventListener,
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

    override fun onSignUp(signupInfo: SignupResponse) {
        _isSigningUp.value = false
        signupEventListener.onSignUp(signupInfo)
    }

    override fun onSignUpFailed(errorResponse: IResponse.Failure<SignupResponse>) {
        _isSigningUp.value = false
        signupEventListener.onSignUpFailed(errorResponse)
    }

    fun cancel() {
        viewModelScope.cancel()
    }
}