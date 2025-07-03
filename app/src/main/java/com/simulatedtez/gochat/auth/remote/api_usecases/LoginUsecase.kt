package com.simulatedtez.gochat.auth.remote.api_usecases

import com.simulatedtez.gochat.auth.remote.api_services.IAuthApiService
import com.simulatedtez.gochat.auth.remote.models.LoginResponse
import com.simulatedtez.gochat.remote.IEndpointCaller
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.RemoteParams
import kotlinx.serialization.Serializable

class LoginUsecase(
    private val authApiService: IAuthApiService
): IEndpointCaller<LoginParams, LoginResponse, IResponse<LoginResponse>> {

    override suspend fun call(
        params: LoginParams,
        handler: IResponseHandler<LoginResponse, IResponse<LoginResponse>>?
    ) {
        handler?.onResponse(authApiService.login(params))
    }
}

data class LoginParams(
    override val request: Request
): RemoteParams(null, request) {

    @Serializable
    class Request(
        val username: String,
        val password: String,
    )
}