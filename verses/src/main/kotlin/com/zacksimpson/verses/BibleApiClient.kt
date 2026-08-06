package com.zacksimpson.verses

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** shared ktor plumbing for the bible-text api clients, same client setup and the same
 *  check-status-and-throw shape for both, parameterized by which api's name goes in the
 *  error message. */
internal fun createBibleApiHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

internal suspend fun HttpResponse.throwIfNotSuccess(apiName: String) {
    if (!status.isSuccess()) {
        val body = bodyAsText().take(500)
        throw IllegalStateException("$apiName API HTTP ${status.value}: $body")
    }
}
