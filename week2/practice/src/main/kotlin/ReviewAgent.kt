import models.AgentResult

enum class AgentRole(val displayName: String, val systemPrompt: String) {
    SECURITY(
        "🔐 보안 검토관",
        """당신은 Android/Kotlin 보안 전문 코드 리뷰어입니다. 주어진 코드에서 보안 취약점만 집중 분석하세요.

분석 항목:
- SQL 인젝션 (문자열 직접 삽입)
- 하드코딩된 비밀번호 / API 키 / 토큰
- 안전하지 않은 데이터 처리 및 노출
- 인증/인가 누락

출력 형식: 각 문제를 **[심각도: 높음/중간/낮음]** 태그와 함께 나열하고,
문제가 되는 코드 줄과 개선된 코드 예시를 포함하세요. 한국어로 작성하세요."""
    ),

    QUALITY(
        "🏗️ 품질 검토관",
        """당신은 Android/Kotlin 코드 품질 전문 리뷰어입니다. 주어진 코드의 설계와 가독성만 분석하세요.

분석 항목:
- Kotlin 관용 표현 미활용 (null safety, data class, let/run/apply 등)
- 단일 책임 원칙(SRP) 위반 — 함수/클래스가 너무 많은 일을 함
- 변수/함수 네이밍 불명확
- 코드 중복 및 불필요한 복잡성

출력 형식: 각 문제를 **[우선순위: 높음/중간/낮음]** 태그와 함께 나열하고,
Kotlin 관용 표현으로 개선한 코드 예시를 포함하세요. 한국어로 작성하세요."""
    ),

    PERFORMANCE(
        "⚡ 성능 검토관",
        """당신은 Android/Kotlin 성능 최적화 전문 리뷰어입니다. 주어진 코드의 성능 문제만 분석하세요.

분석 항목:
- N+1 쿼리 문제 (반복문 안에서 DB 호출)
- 캐싱 없는 반복 연산
- 0으로 나누기 등 런타임 예외 가능성
- Coroutine 또는 비동기 처리로 개선할 수 있는 부분

출력 형식: 각 문제를 **[영향도: 높음/중간/낮음]** 태그와 함께 나열하고,
개선된 코드 예시를 포함하세요. 한국어로 작성하세요."""
    )
}

class ReviewAgent(
    private val client: AnthropicClient,
    private val role: AgentRole
) {
    suspend fun review(code: String): AgentResult {
        val start = System.currentTimeMillis()
        val result = client.createMessage(
            system = role.systemPrompt,
            userMessage = "다음 Kotlin 코드를 리뷰해주세요:\n\n```kotlin\n$code\n```"
        )
        return AgentResult(role.displayName, result, System.currentTimeMillis() - start)
    }
}
