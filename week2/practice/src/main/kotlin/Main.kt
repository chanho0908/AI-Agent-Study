import kotlinx.coroutines.runBlocking
import models.ReviewReport
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun main() = runBlocking {
    val apiKey = System.getenv("ANTHROPIC_API_KEY")
        ?: error("ANTHROPIC_API_KEY 환경변수가 설정되지 않았습니다.\n  export ANTHROPIC_API_KEY=sk-ant-...")

    val sampleFile = File("sample/UserRepository.kt")
    require(sampleFile.exists()) { "sample/UserRepository.kt 파일이 없습니다" }

    println("📂 리뷰 대상: ${sampleFile.path}")
    println()

    val client = AnthropicClient(apiKey)
    try {
        val report = Orchestrator(client).review(sampleFile.readText())
        report.printToConsole()
        report.saveToMarkdown()
    } finally {
        client.close()
    }
}

fun ReviewReport.printToConsole() {
    println("\n${"═".repeat(52)}")
    println("📊 최종 리뷰 리포트")
    println("═".repeat(52))

    agentResults.forEach { result ->
        println("\n${result.agentName}  (${result.elapsedMs}ms)")
        println("─".repeat(40))
        println(result.review)
    }

    println("\n${"═".repeat(52)}")
    println("🎯 오케스트레이터 종합 요약")
    println("═".repeat(52))
    println(summary)

    val parallelAgentTime = agentResults.maxOf { it.elapsedMs }
    val sequentialAgentTime = agentResults.sumOf { it.elapsedMs }
    println("\n${"─".repeat(52)}")
    println("⏱️  총 실행 시간 (에이전트 + 취합) : ${totalElapsedMs}ms")
    println("   에이전트 병렬 실행 시간        : ${parallelAgentTime}ms")
    println("   에이전트 순차 예상 시간        : ~${sequentialAgentTime}ms")
    println("   병렬화로 절약                : ~${sequentialAgentTime - parallelAgentTime}ms  🚀")
    println("─".repeat(52))
}

fun ReviewReport.saveToMarkdown() {
    File("results").mkdirs()
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"))
    val file = File("results/review_$timestamp.md")

    file.writeText(buildString {
        appendLine("# 멀티 에이전트 코드 리뷰 결과")
        appendLine()
        appendLine("| 항목 | 값 |")
        appendLine("|------|-----|")
        appendLine("| 생성 시각 | $timestamp |")
        val parallelAgentTime = agentResults.maxOf { it.elapsedMs }
        val sequentialAgentTime = agentResults.sumOf { it.elapsedMs }
        appendLine("| 총 실행 시간 (에이전트 + 취합) | ${totalElapsedMs}ms |")
        appendLine("| 에이전트 병렬 실행 시간 | ${parallelAgentTime}ms |")
        appendLine("| 에이전트 순차 예상 시간 | ~${sequentialAgentTime}ms |")
        appendLine("| 병렬화로 절약 | ~${sequentialAgentTime - parallelAgentTime}ms |")
        appendLine()
        appendLine("---")
        appendLine()

        agentResults.forEach { result ->
            appendLine("## ${result.agentName}")
            appendLine()
            appendLine("> 처리 시간: ${result.elapsedMs}ms")
            appendLine()
            appendLine(result.review)
            appendLine()
            appendLine("---")
            appendLine()
        }

        appendLine("## 🎯 오케스트레이터 종합 요약")
        appendLine()
        appendLine(summary)
    })

    println("\n💾 결과 저장 완료: ${file.path}")
}
