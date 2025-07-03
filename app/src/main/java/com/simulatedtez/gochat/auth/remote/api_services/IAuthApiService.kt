package com.simulatedtez.gochat.auth.remote.api_services

import com.simulatedtez.gochat.auth.remote.api_usecases.LoginParams
import com.simulatedtez.gochat.auth.remote.api_usecases.SignupParams
import com.simulatedtez.gochat.auth.remote.models.LoginResponse
import com.simulatedtez.gochat.auth.remote.models.SignupResponse
import com.simulatedtez.gochat.remote.IResponse

interface IAuthApiService {
    suspend fun login(params: LoginParams): IResponse<LoginResponse>
    suspend fun signup(params: SignupParams): IResponse<SignupResponse>
}