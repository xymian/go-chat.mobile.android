package com.simulatedtez.gochat

open class Session private constructor(
    var username: String = "",
    var accessToken: String = ""
) {
    companion object {
        val session = object: Session() {

        }
    }

    fun saveAccessToken(token: String) {
        accessToken = token
    }

    fun saveUsername(username: String) {
        this.username = username
    }
}