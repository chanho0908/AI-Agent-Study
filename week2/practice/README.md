# 멀티 에이전트 코드 리뷰 팀 실습

## 팀 구성

```
오케스트레이터 (Claude Sonnet)
├── 🔐 보안 검토관  (Claude Haiku) ┐
├── 🏗️ 품질 검토관  (Claude Haiku) ├── 병렬 실행
└── ⚡ 성능 검토관  (Claude Haiku) ┘
         ↓
   결과 취합 → 최종 리뷰 리포트 (Markdown)
```

각 에이전트는 **독립적인 시스템 프롬프트**로 전문화되어 있고,
Kotlin Coroutines의 `async/awaitAll`로 **동시에 실행**된다.

## 프로젝트 구조

```
practice/
├── build.gradle.kts
├── settings.gradle.kts
├── src/main/kotlin/
│   ├── Main.kt              # 진입점 + 결과 출력/저장
│   ├── AnthropicClient.kt   # Anthropic REST API 클라이언트 (Ktor)
│   ├── ReviewAgent.kt       # 에이전트 역할 정의 + 실행
│   ├── Orchestrator.kt      # 병렬 실행 조율 + 결과 취합
│   └── models/Models.kt     # 데이터 클래스
├── sample/
│   └── UserRepository.kt    # 리뷰 대상 (의도적 문제 포함)
└── results/
    └── review_*.md          # 실행 결과 (자동 생성)
```

## 실행 방법

```bash
export ANTHROPIC_API_KEY=sk-ant-...
gradle run
```

## 핵심 코드: 병렬 실행

```kotlin
// Orchestrator.kt
val results: List<AgentResult> = coroutineScope {
    AgentRole.entries.map { role ->
        async {                          // 각 에이전트를 동시에 시작
            ReviewAgent(client, role).review(code)
        }
    }.awaitAll()                         // 전부 끝날 때까지 대기
}
```

`async { }` 블록 3개가 동시에 시작되고,
`awaitAll()` 이 모두 끝날 때까지 기다린다.
결과적으로 가장 느린 에이전트 시간만큼만 소요된다.

## 실행 결과 요약

| 항목 | 측정값 |
|------|-------|
| 에이전트 병렬 실행 | ~8.4초 (가장 느린 에이전트 기준) |
| 에이전트 순차 예상 | ~23.9초 |
| 병렬화로 절약 | **~15.5초** |
| 오케스트레이터 취합 | ~30초 (Claude Sonnet) |
| 전체 총 실행 시간 | ~39초 |

→ 실제 결과: [results/](./results/) 폴더 참고

## 샘플 코드에 심은 문제들

`sample/UserRepository.kt`에는 의도적으로 다음 문제들이 포함됨:

| 종류 | 문제 | 위치 |
|------|------|------|
| 🔐 보안 | SQL 인젝션 (문자열 직접 삽입) | `findUserByName`, `processUser`, `getUserStats` |
| 🔐 보안 | 하드코딩된 비밀번호 / JWT 토큰 | 클래스 프로퍼티 |
| 🏗️ 품질 | SRP 위반 (7가지 책임을 1개 함수에) | `processUser` |
| 🏗️ 품질 | 의미 없는 변수명 (`n`, `e`, `a`, `c`) | `processUser` |
| ⚡ 성능 | N+1 쿼리 | `getAllUsersWithOrders` |
| ⚡ 성능 | 0으로 나누기 (ArithmeticException) | `getUserStats` |
| ⚡ 성능 | 캐싱 없는 반복 계산 | `getUserStats` |
