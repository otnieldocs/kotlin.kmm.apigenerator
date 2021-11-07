package me.ootniel.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UserLoginResponse(
	@SerialName("auth_token")
	val authToken: String,
	@SerialName("refresh_token")
	val refreshToken: String
)