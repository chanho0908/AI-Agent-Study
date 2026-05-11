package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Anthropic API 요청/응답 ──────────────────────────────────

@Serializable
data class ApiMessage(
    val role: String,
    val content: String
)

@Serializable
data class MessageRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val system: String,
    val messages: List<ApiMessage>
)

@Serializable
data class ContentBlock(
    val type: String,
    val text: String = ""
)

@Serializable
data class ErrorDetail(
    val type: String = "",
    val message: String = ""
)

@Serializable
data class MessageResponse(
    val content: List<ContentBlock> = emptyList(),
    val type: String? = null,
    val error: ErrorDetail? = null
)

// ── 에이전트 결과 ────────────────────────────────────────────

data class AgentResult(
    val agentName: String,
    val review: String,
    val elapsedMs: Long
)

data class ReviewReport(
    val agentResults: List<AgentResult>,
    val summary: String,
    val totalElapsedMs: Long
) {
    val sequentialEstimateMs: Long get() = agentResults.sumOf { it.elapsedMs }
    val savedMs: Long get() = sequentialEstimateMs - totalElapsedMs
}
