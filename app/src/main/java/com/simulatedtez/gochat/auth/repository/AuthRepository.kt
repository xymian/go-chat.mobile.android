package com.simulatedtez.gochat.auth.repository

import com.simulatedtez.gochat.auth.remote.api_usecases.LoginParams
import com.simulatedtez.gochat.auth.remote.api_usecases.LoginUsecase
import com.simulatedtez.gochat.auth.remote.api_usecases.SignupParams
import com.simulatedtez.gochat.auth.remote.api_usecases.SignupUsecase
import com.simulatedtez.gochat.auth.remote.models.LoginResponse
import com.simulatedtez.gochat.auth.remote.models.SignupResponse
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.Response

class AuthRepository(
    private val loginUsecase: LoginUsecase,
    private val signupUsecase: SignupUsecase,
    private val authEventListener: AuthEventListener,
) {

    suspend fun login(username: String, password: String) {
        val loginParams = LoginParams(
            request = LoginParams.Request(
                username = username,
                password = password
            )
        )
        loginUsecase.call(
            loginParams, object: IResponseHandler<LoginResponse, IResponse<LoginResponse>> {
                override fun onResponse(response: IResponse<LoginResponse>) {
                    when (response) {
                        is IResponse.Success -> {
                            authEventListener.onLogin(response.data)
                            // cache login details
                        }
                        is IResponse.Failure -> {
                            authEventListener.onLoginFailed(response)
                        }

                        is Response -> {}
                    }
                }
            }
        )
    }

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
                            authEventListener.onSignUp(response.data)
                        }
                        is IResponse.Failure -> {
                            authEventListener.onSignUpFailed(response)
                        }

                        is Response -> {}
                    }
                }
            }
        )
    }
}

interface AuthEventListener {
    fun onSignUpFailed(errorResponse: IResponse.Failure<SignupResponse>)
    fun onLoginFailed(errorResponse: IResponse.Failure<LoginResponse>)
    fun onLogin(loginInfo: LoginResponse)
    fun onSignUp(signupInfo: SignupResponse)
}