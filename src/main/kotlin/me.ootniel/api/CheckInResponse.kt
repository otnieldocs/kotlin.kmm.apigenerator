package me.ootniel.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class CheckInResponse(
	@SerialName("created_at")
	val createdAt: String
)