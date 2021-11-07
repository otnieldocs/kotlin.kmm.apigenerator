package me.ootniel.api

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import me.ootniel.api.RestApiConfiguration.BASE_URL

class RestApiCollection(private val client: HttpClient) {
    suspend fun callPostUserLogin(request: UserLoginRequest): UserLoginResponse? {
        return client.post("${BASE_URL}/login") {
            contentType(ContentType.Application.Json)
            body = request
        }
    }
	suspend fun callPostUserLogin2(request: UserLogin2Request): UserLogin2Response? {
        return client.post("${BASE_URL}/login") {
            contentType(ContentType.Application.Json)
            body = request
        }
    }
	suspend fun callPostCheckIn(): CheckInResponse? {
        return client.post("${BASE_URL}/checkin") {
            contentType(ContentType.Application.Json)
        }
    }
	
}