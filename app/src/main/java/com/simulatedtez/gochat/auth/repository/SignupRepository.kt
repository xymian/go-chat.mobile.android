package com.simulatedtez.gochat.auth.repository

import com.simulatedtez.gochat.auth.remote.api_usecases.SignupParams
import com.simulatedtez.gochat.auth.remote.api_usecases.SignupUsecase
import com.simulatedtez.gochat.auth.remote.models.SignupResponse
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SignupRepository(
    private val signupUsecase: SignupUsecase,
    private val signupEventListener: SignupEventListener,
) {

    private val context = CoroutineScope(Dispatchers.Default + SupervisorJob())

    suspend fun signUp(username: String, password: String) {
        val signupParams = SignupParams(
            request = SignupParams.Request(
                username = username,
                password = password
            )
        )
        signupUsecase.call(
            signupParams, object: IResponseHandler<SignupResponse, IResponse<SignupResponse>> {
                override fun onResponse(response: IResponse<SignupResponse>) {
                    when (response) {
                        is IResponse.Success -> {
                            // cache login details
                            context.launch(Dispatchers.Main) {
                                signupEventListener.onSignUp(response.data)
                            }
                        }
                        is IResponse.Failure -> {
                            context.launch(Dispatchers.Main) {
                                signupEventListener.onSignUpFailed(response)
                            }
                        }

                        is Response -> {}
                    }
                }
            }
        )
    }

    fun cancel() {
        context.cancel()
    }
}

interface SignupEventListener {
    fun onSignUp(signupInfo: SignupResponse)
    fun onSignUpFailed(errorResponse: IResponse.Failure<SignupResponse>)
}