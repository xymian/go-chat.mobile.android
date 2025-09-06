package com.simulatedtez.gochat.auth.repository

import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.UserPreference
import com.simulatedtez.gochat.auth.remote.api_usecases.LoginParams
import com.simulatedtez.gochat.auth.remote.api_usecases.LoginUsecase
import com.simulatedtez.gochat.auth.remote.models.LoginResponse
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.Response
import com.simulatedtez.gochat.utils.CleanupManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LoginRepository(
    private val loginUsecase: LoginUsecase,
    private val cleanupManager: CleanupManager
) {
    private var loginEventListener: LoginEventListener? = null

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    suspend fun login(username: String, password: String) {
        val loginParams = LoginParams(
            request = LoginParams.Request(
                username = username,
                password = password
            )
        )
        loginUsecase.call(
            loginParams, object: IResponseHandler<ParentResponse<LoginResponse>,
                    IResponse<ParentResponse<LoginResponse>>> {
                override fun onResponse(response: IResponse<ParentResponse<LoginResponse>>) {
                    when (response) {
                        is IResponse.Success -> {
                            scope.launch(Dispatchers.Main) {
                                response.data.data?.let {
                                    if (session.username != username) {
                                        scope.launch(Dispatchers.IO) {
                                            cleanupManager.clearUserData()
                                        }
                                        UserPreference.storeUsername(username)
                                        UserPreference.storeAccessToken(it.accessToken)
                                        session.saveAccessToken(it.accessToken)
                                        session.saveUsername(username)
                                    }
                                    loginEventListener?.onLogin(it)
                                }
                            }
                        }
                        is IResponse.Failure -> {
                            scope.launch(Dispatchers.Main) {
                                loginEventListener?.onLoginFailed(response)
                            }
                        }

                        is Response -> {}
                    }
                }
            }
        )
    }

    fun setEventListener(listener: LoginEventListener) {
        loginEventListener = listener
    }

    fun cancel() {
        scope.cancel()
    }
}

interface LoginEventListener {
    fun onLogin(loginInfo: LoginResponse)
    fun onLoginFailed(errorResponse: IResponse.Failure<ParentResponse<LoginResponse>>)
}