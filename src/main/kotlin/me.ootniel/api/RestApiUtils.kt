package me.ootniel.api

import io.ktor.http.*

object RestApiUtils {
    fun toParameters(params: Map<String, String>): Parameters {
        val query = params.toList().formUrlEncode()

        return parseQueryString(query)
    }
}