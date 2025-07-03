package com.simulatedtez.gochat.auth.repository

import com.simulatedtez.gochat.auth.remote.api_usecases.LoginParams
import com.simulatedtez.gochat.auth.remote.api_usecases.LoginUsecase
import com.simulatedtez.gochat.auth.remote.models.LoginResponse
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.Response

class LoginRepository(
    private val loginUsecase: LoginUsecase,
    private val loginEventListener: LoginEventListener,
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
                            loginEventListener.onLogin(response.data)
                            // cache login details
                        }
                        is IResponse.Failure -> {
                            loginEventListener.onLoginFailed(response)
                        }

                        is Response -> {}
                    }
                }
            }
        )
    }
}

interface LoginEventListener {
    fun onLogin(loginInfo: LoginResponse)
    fun onLoginFailed(errorResponse: IResponse.Failure<LoginResponse>)
}