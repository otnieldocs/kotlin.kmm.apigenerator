package me.ootniel.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class GetAllUserResponse(
	@SerialName("username")
	val username: String
)