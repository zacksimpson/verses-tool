package com.zacksimpson.verses

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Shared Ktor plumbing for the Bible-text API clients (EsvApi, YouVersionApi) — same
 *  client setup and the same "check status, throw with a truncated body on failure" shape
 *  for both, parameterized only by which API's name to put in the error message. */
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
