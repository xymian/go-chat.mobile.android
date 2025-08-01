package com.simulatedtez.gochat.auth.remote.api_services

import com.simulatedtez.gochat.BuildConfig
import com.simulatedtez.gochat.auth.remote.api_usecases.LoginParams
import com.simulatedtez.gochat.auth.remote.api_usecases.SignupParams
import com.simulatedtez.gochat.auth.remote.models.LoginResponse
import com.simulatedtez.gochat.auth.remote.models.SignupResponse
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.Response
import com.simulatedtez.gochat.remote.postWithBaseUrl
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AuthApiService(private val client: HttpClient): IAuthApiService {

    override suspend fun login(params: LoginParams): IResponse<ParentResponse<LoginResponse>> {
        return Response< ParentResponse<LoginResponse>> {
            client.postWithBaseUrl("/login") {
                contentType(ContentType.Application.Json)
                setBody(params.request)
            }
        }.invoke()
    }

    override suspend fun signup(params: SignupParams): IResponse<ParentResponse<String>> {
        return Response<ParentResponse<SignupResponse>> {
            client.postWithBaseUrl("/register") {
                contentType(ContentType.Application.Json)
                setBody(params.request)
            }
        }.invoke()
    }
}