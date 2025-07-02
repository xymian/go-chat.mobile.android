package com.simulatedtez.gochat.remote

interface IEndpointCaller<Q, D, R:IResponse<D>> {

    suspend fun call(request: Q, handler: IResponseHandler<D, R>?)
}