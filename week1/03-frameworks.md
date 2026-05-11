# 주요 AI Agent 프레임워크 비교

AI Agent를 만들 때 처음부터 다 직접 구현할 수도 있지만, 대부분 프레임워크를 활용한다.
이 문서는 현재 가장 많이 쓰이는 프레임워크들의 역할과 특징을 정리한다.

---

## 프레임워크가 왜 필요한가?

Agent를 직접 만들면 아래 것들을 전부 구현해야 한다.

- LLM API 호출 관리
- 도구(Tool) 정의 및 실행
- 반복 루프 (Thought → Action → Observation)
- 메모리 관리
- 에러 처리

프레임워크는 이 반복 작업들을 추상화해서 **Agent 로직 설계에 집중**할 수 있게 해준다.

---

## LangChain

**한 줄 요약:** LLM 기반 애플리케이션을 만들기 위한 범용 도구 모음

- 2022년 등장, 현재 가장 널리 쓰이는 프레임워크
- LLM, 도구, 메모리, 프롬프트 등을 **체인(Chain)** 형태로 연결
- OpenAI, Claude, Gemini 등 다양한 LLM 지원

**장점:**
- 문서와 커뮤니티가 풍부함
- 다양한 도구, 벡터 DB, LLM과 통합 가능

**단점:**
- 추상화가 많아서 내부 동작을 이해하기 어려울 수 있음
- 간단한 작업도 코드가 복잡해지는 경향

**적합한 상황:** RAG(문서 검색 기반 Q&A), 단순한 도구 사용 Agent

```python
# LangChain으로 도구를 가진 Agent 만드는 개념적 예시
from langchain.agents import create_react_agent
from langchain.tools import Tool

tools = [
    Tool(name="search", func=search_web, description="웹 검색")
]
agent = create_react_agent(llm, tools, prompt)
agent.invoke({"input": "서울 날씨 알려줘"})
```

---

## LangGraph

**한 줄 요약:** Agent의 흐름을 **그래프(Graph)** 형태로 설계하는 프레임워크

- LangChain 팀이 만든 상위 레벨 프레임워크
- Agent의 상태와 흐름을 **노드(Node)** 와 **엣지(Edge)** 로 표현

```
[시작] → [검색] → [분석] → [조건 분기] → [답변 생성] → [종료]
                              ↓ (실패시)
                           [재시도]
```

**장점:**
- 복잡한 흐름 제어가 명확함 (조건 분기, 반복, 병렬 처리)
- 각 단계가 눈에 보여서 디버깅이 쉬움
- 인간 개입(Human-in-the-loop) 구현이 쉬움

**단점:**
- LangChain보다 학습 곡선이 높음

**적합한 상황:** 복잡한 멀티스텝 Agent, 조건 분기가 많은 워크플로우, 멀티 Agent 시스템

---

## LangChain vs LangGraph 선택 기준

| 상황 | 추천 |
|------|------|
| 처음 배울 때 | LangChain |
| 단순한 RAG, 단일 도구 | LangChain |
| 복잡한 흐름 제어 필요 | LangGraph |
| 멀티 Agent 시스템 | LangGraph |
| 중간에 사람이 개입해야 함 | LangGraph |

---

## Koog (JetBrains)

**한 줄 요약:** JetBrains가 만든 Kotlin 기반 AI Agent 프레임워크

- Kotlin/JVM 환경에서 Agent를 만들기 위한 프레임워크
- 안드로이드, 서버 등 JVM 생태계와 잘 통합됨
- Python 중심인 LangChain/LangGraph와 달리 JVM 개발자를 위한 선택지

---

## 모니터링 도구

Agent를 만들면 "잘 동작하고 있나?" 를 확인해야 한다.

### Langfuse

- Agent의 실행 로그, LLM 호출, 도구 사용을 추적
- 어떤 프롬프트가 어떤 결과를 냈는지 시각화
- 오픈소스라 자체 서버에 설치 가능

### LangSmith

- LangChain 팀이 만든 공식 모니터링 도구
- LangChain/LangGraph와 통합이 자연스러움
- 프롬프트 실험, A/B 테스트 기능 제공

---

## 프레임워크 없이 직접 만드는 것도 선택지

프레임워크는 편리하지만 필수는 아니다. Claude API나 OpenAI API를 직접 호출해서 Agent를 만들 수도 있다.

**직접 구현이 맞는 상황:**
- 프레임워크의 추상화가 오히려 방해될 때
- 아주 특수한 요구사항이 있을 때
- 내부 동작을 완전히 이해하고 싶을 때

**처음 배울 때 권장 경로:**
1. 개념 이해 (지금 하고 있는 것)
2. Claude API 직접 호출로 간단한 Tool Use 구현 → 내부 동작 이해
3. LangGraph로 복잡한 흐름 설계

---

## 정리

| 프레임워크 | 언어 | 특징 | 추천 용도 |
|-----------|------|------|----------|
| LangChain | Python | 범용, 커뮤니티 풍부 | RAG, 단순 Agent |
| LangGraph | Python | 그래프 기반 흐름 제어 | 복잡한 Agent, 멀티 Agent |
| Koog | Kotlin | JVM 생태계 | Android, JVM 서버 |
| (직접 구현) | 아무거나 | 완전한 제어권 | 학습, 특수 요구사항 |
