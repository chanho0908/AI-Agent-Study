import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import models.*

class AnthropicClient(private val apiKey: String) {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 120_000
        }
    }

    suspend fun createMessage(
        system: String,
        userMessage: String,
        model: String = "claude-haiku-4-5-20251001",
        maxTokens: Int = 1024
    ): String {
        val response: MessageResponse = httpClient.post("https://api.anthropic.com/v1/messages") {
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            contentType(ContentType.Application.Json)
            setBody(
                MessageRequest(
                    model = model,
                    maxTokens = maxTokens,
                    system = system,
                    messages = listOf(ApiMessage("user", userMessage))
                )
            )
        }.body()

        if (response.type == "error" || response.content.isEmpty()) {
            val msg = response.error?.message ?: "알 수 없는 API 오류"
            error("Anthropic API 오류: $msg")
        }
        return response.content.first { it.type == "text" }.text
    }

    fun close() = httpClient.close()
}
