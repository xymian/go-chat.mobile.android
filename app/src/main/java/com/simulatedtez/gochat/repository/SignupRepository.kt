package com.simulatedtez.gochat.repository

import com.simulatedtez.gochat.remote.api_usecases.SignupParams
import com.simulatedtez.gochat.remote.api_usecases.SignupUsecase
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SignupRepository(
    private val signupUsecase: SignupUsecase,
) {
    private val context = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var signupEventListener: SignupEventListener? = null

    fun setEventListener(listener: SignupEventListener) {
        signupEventListener = listener
    }

    suspend fun signUp(username: String, password: String) {
        val signupParams = SignupParams(
            request = SignupParams.Request(
                username = username,
                password = password
            )
        )
        signupUsecase.call(
            signupParams, object: IResponseHandler<ParentResponse<String>,
                    IResponse<ParentResponse<String>>> {
                override fun onResponse(response: IResponse<ParentResponse<String>>) {
                    when (response) {
                        is IResponse.Success -> {
                            context.launch(Dispatchers.Main) {
                                response.data.data?.let {
                                    signupEventListener?.onSignUp()
                                }
                            }
                        }
                        is IResponse.Failure -> {
                            context.launch(Dispatchers.Main) {
                                signupEventListener?.onSignUpFailed(response)
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
    fun onSignUp()
    fun onSignUpFailed(errorResponse: IResponse.Failure<ParentResponse<String>>)
}