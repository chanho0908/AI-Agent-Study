import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import models.AgentResult
import models.ReviewReport

class Orchestrator(private val client: AnthropicClient) {

    suspend fun review(code: String): ReviewReport {
        printHeader()
        val totalStart = System.currentTimeMillis()

        // ── 핵심: 3개 에이전트 병렬 실행 ──────────────────────
        // async { } 로 각 에이전트를 동시에 시작하고
        // awaitAll() 로 전부 끝날 때까지 기다린다
        val results: List<AgentResult> = coroutineScope {
            AgentRole.entries.map { role ->
                async {
                    println("  ▶ ${role.displayName} 시작")
                    val result = ReviewAgent(client, role).review(code)
                    println("  ✅ ${role.displayName} 완료 (${result.elapsedMs}ms)")
                    result
                }
            }.awaitAll()
        }

        println("\n📋 오케스트레이터: 결과 취합 중...")
        val summary = synthesize(results)
        val totalElapsed = System.currentTimeMillis() - totalStart

        return ReviewReport(results, summary, totalElapsed)
    }

    private suspend fun synthesize(results: List<AgentResult>): String {
        val combined = results.joinToString("\n\n---\n\n") {
            "### ${it.agentName}\n\n${it.review}"
        }
        return client.createMessage(
            system = """당신은 수석 Android 개발자입니다.
세 전문가(보안/품질/성능)의 코드 리뷰를 종합해 최종 요약을 작성하세요.

다음 구성으로 작성하세요:
1. **총평** — 코드 품질 점수 (x / 10) 와 한 줄 요약
2. **즉시 수정 필요 TOP 3** — 가장 심각한 문제 3가지
3. **카테고리별 핵심 포인트** — 보안 / 품질 / 성능 각 1~2줄
4. **다음 단계 권고** — 우선순위 순으로 할 일 목록

한국어로 작성하세요.""",
            userMessage = "다음 전문가 리뷰를 종합해주세요:\n\n$combined",
            model = "claude-sonnet-4-6",
            maxTokens = 2048
        )
    }

    private fun printHeader() {
        println("""
╔══════════════════════════════════════════════╗
║      멀티 에이전트 코드 리뷰 팀 가동         ║
╠══════════════════════════════════════════════╣
║  🔐 보안 검토관    │ Claude Haiku (병렬)     ║
║  🏗️  품질 검토관   │ Claude Haiku (병렬)     ║
║  ⚡ 성능 검토관    │ Claude Haiku (병렬)     ║
║  🎯 오케스트레이터  │ Claude Sonnet (취합)   ║
╚══════════════════════════════════════════════╝
        """.trimIndent())
    }
}
