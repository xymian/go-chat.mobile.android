package com.simulatedtez.gochat.auth.remote.api_usecases

import com.simulatedtez.gochat.auth.remote.api_services.IAuthApiService
import com.simulatedtez.gochat.auth.remote.models.SignupResponse
import com.simulatedtez.gochat.remote.IEndpointCaller
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.RemoteParams
import kotlinx.serialization.Serializable

class SignupUsecase(
    private val authApiService: IAuthApiService
): IEndpointCaller<SignupParams, ParentResponse<String>, IResponse<ParentResponse<String>>> {

    override suspend fun call(
        params: SignupParams,
        handler: IResponseHandler<ParentResponse<String>, IResponse<ParentResponse<String>>>?
    ) {
        handler?.onResponse(authApiService.signup(params))
    }
}

data class SignupParams(
    override val request: Request
): RemoteParams(null, request) {

    @Serializable
    data class Request(
        val username: String,
        val password: String,
    )
}