package com.simulatedtez.gochat.auth.repository

import com.simulatedtez.gochat.auth.remote.api_usecases.SignupParams
import com.simulatedtez.gochat.auth.remote.api_usecases.SignupUsecase
import com.simulatedtez.gochat.auth.remote.models.SignupResponse
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.Response

class SignupRepository(
    private val signupUsecase: SignupUsecase,
    private val signupEventListener: SignupEventListener,
) {

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
                            signupEventListener.onSignUp(response.data)
                        }
                        is IResponse.Failure -> {
                            signupEventListener.onSignUpFailed(response)
                        }

                        is Response -> {}
                    }
                }
            }
        )
    }
}

interface SignupEventListener {
    fun onSignUp(signupInfo: SignupResponse)
    fun onSignUpFailed(errorResponse: IResponse.Failure<SignupResponse>)
}