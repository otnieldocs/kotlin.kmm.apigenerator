package com.otnieldocs.app

import me.ootniel.api.RestApiCollection
import me.ootniel.api.RestApiConfiguration
import me.ootniel.api.UserLoginRequest

suspend fun main() {
    val apiClient = RestApiConfiguration.client
    val apiCollection = RestApiCollection(apiClient)
    val res = apiCollection.callPostUserLogin(UserLoginRequest("otniel123", "12345678"))
    println("res = ${res?.authToken}, ${res?.refreshToken}")

    val res2 = apiCollection.callPostCheckIn()
    println("res2 = ${res2?.createdAt}")
}