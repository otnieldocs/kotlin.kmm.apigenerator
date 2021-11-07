package me.ootniel.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UserLogin2Response(
	@SerialName("auth_token")
	val authToken: String,
	@SerialName("refresh_token")
	val refreshToken: String
)