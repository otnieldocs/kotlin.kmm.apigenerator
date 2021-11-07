package me.ootniel.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UserLogin2Request(
	@SerialName("username")
	val username: String,
	@SerialName("password")
	val password: String
)