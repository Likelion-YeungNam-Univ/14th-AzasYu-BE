# SyncBridge

가짜합의(Fake Agreement) 방지 AI 협업 플랫폼. 구현 기준은 [`SPEC.md`](./SPEC.md).

```
prototyping/index.html ──REST/SSE──► backend (Spring Boot 3.3) ──► PostgreSQL
                                          │
                                          └──WebClient──► ai-service (FastAPI + LangChain GPT-4o)
```

## 1. 실행 방법

### 1-1. AI Microservice (FastAPI, :8000)

```bash
cd ai-service
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env          # OPENAI_API_KEY 입력 (없으면 Mock 모드로 동작)
uvicorn app.main:app --reload --port 8000
```

- Swagger: http://localhost:8000/docs
- 헬스 체크: http://localhost:8000/health → `llmEnabled` 로 Mock/실제 모드 확인
- 테스트: `pytest`

> **Mock 모드**: `OPENAI_API_KEY` 가 없으면 질문 스트리밍/회의록 분석이 규칙 기반 응답으로 대체된다.
> 키 없이도 SSE 타자기 효과와 파일 업로드 파이프라인을 그대로 검증할 수 있다.

### 1-2. Main Backend (Spring Boot, :8080)

PostgreSQL 15+ 가 필요하다.

```bash
createdb syncbridge   # 또는 docker run -e POSTGRES_DB=syncbridge -p 5432:5432 postgres:15

cd backend
./gradlew build -x test
./gradlew bootRun
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- 테스트: `./gradlew test`
- 접속 정보는 환경변수로 덮어쓸 수 있다: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `AI_SERVICE_URL`, `FILE_UPLOAD_DIR`

> **JDK 주의:** Gradle 8.8 은 JDK 21 이하에서 실행해야 한다. JDK 26 이 기본이라면
> `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew build` 처럼 지정한다.
> (컴파일 타깃은 toolchain 설정으로 항상 Java 17)

### 1-3. Prototyping Client

Spring Boot 가 `prototyping/` 디렉터리를 정적 리소스로 서빙하므로 별도 서버가 필요 없다.

```
http://localhost:8080/prototyping/index.html
```

검증 순서: **0. 회원가입 → 로그인 → 1. SSE 질문 스트리밍/답변 제출 → 2. 회의록 업로드/결과 조회 → 3. 익명 아이디어 보드**

## 2. 모듈 구조

```
backend/src/main/java/com/syncbridge/app/
├── global/{config, security, error}     # SecurityConfig, SwaggerConfig, WebClientConfig, JWT, 예외 처리
└── domain/
    ├── auth/        회원가입·로그인·로그아웃(JWT)
    ├── project/     프로젝트 생성/목록/참여 코드(JoinCodeGenerator)
    ├── meeting/     회의 생성/상세 대시보드
    ├── interview/   SSE 스트리밍 프록시(WebClient) + 답변 저장
    ├── ideaboard/   소프트 익명 아이디어 보드
    └── result/      회의록 업로드(FileStorageService) + 비동기 AI 분석 + 리포트

ai-service/app/
├── main.py          FastAPI + CORS
├── routers/         interview.py(SSE), analysis.py(파일 파싱→구조화 추출)
├── services/        parser.py, prompt_templates.py, llm_chain.py
└── schemas/         Pydantic v2 모델
```

## 3. 핵심 구현 포인트

### 소프트 익명성 (IdeaService)
`idea_card.user_id` 는 DB 에 그대로 저장하고(개인 성과 반영 대비), 외부로 나가는
`IdeaResponseDto` 로 변환하는 시점에만 `authorName` 을 `"익명"` 으로 마스킹한다.
AI 카드는 `"AI 챗봇"`, `is_revealed = true` 로 공개 전환된 카드만 실명을 노출한다.

### SSE 스트리밍 프록시 (InterviewController/Service)
Spring 이 DB 에서 인터뷰 컨텍스트(회의 목적/안건 + 사용자의 이전 답변 + 직전 회의의 오해 리스크)를
조립해 FastAPI 로 POST 하고, 응답 스트림을 `WebClient.bodyToFlux(ServerSentEvent)` 로 받아
클라이언트에 그대로 중계한다. 업스트림이 `DONE` 없이 끊겨도 종료 이벤트를 보정 전송하고,
호출 실패 시 `{"status":"ERROR", ...}` 이벤트 후 `DONE` 을 보내 EventSource 가 닫히도록 한다.

### 선순환 구조
회의록 분석에서 감지된 `misunderstandings` 는 `[AI 리스크 감지]` 접두사를 붙여
해당 회의의 아이디어 보드에 AI 카드로 자동 게시된다.

### 태스크별 모델 분리 (AI 비용 최적화)
두 기능은 요구되는 추론 난이도가 다르므로 모델을 분리한다.

| 태스크 | 환경변수 | 기본값 | 근거 |
| --- | --- | --- | --- |
| 인터뷰 질문 생성 | `OPENAI_MODEL_INTERVIEW` | `gpt-4o-mini` | 출력 23토큰 수준의 규칙 준수형 짧은 생성 |
| 회의록 분석 | `OPENAI_MODEL_ANALYSIS` | `gpt-5.6-terra` | '말해지지 않은 전제'를 찾는 화용론적 추론 |

실측 기준 회의 1건(참석자 6명)당 input 약 30K / output 약 1.2K 토큰이며,
이 중 **인터뷰가 input 의 95%** 를 차지한다(6명 × 6문항 = 36회 호출). 분리 구성 시
회의당 약 $0.012 로, 전 구간 `gpt-4o` 대비 약 7배 저렴하면서 분석 품질은 더 높다.

### 인터뷰 질문 캐싱 (중복 과금 방지)
스트림 엔드포인트는 호출될 때마다 LLM 을 새로 부르므로, 새로고침/재접속이 그대로 중복 과금이 된다.
생성된 질문을 `interview_question` 테이블에 `(meeting, user, questionNum)` 단위로 저장하고,
재요청 시 LLM 호출 없이 동일한 타자기 효과로 재생한다.
질문 N 은 답변 1..N-1 을 컨텍스트로 생성되므로, 답변이 제출되면 그보다 뒤 번호의 캐시는 자동 무효화된다.

### 프롬프트 캐싱 활성화
OpenAI 자동 프롬프트 캐싱은 **1,024 토큰 이상** 프롬프트에만 적용된다. 인터뷰 시스템 프롬프트는
few-shot 예시 7개를 포함해 현재 약 1,287 토큰이며(캐시 구간 1,280), 모든 인터뷰 호출이 이를
공통 접두부로 공유한다. 길이를 줄이면 캐시가 통째로 꺼지므로
`ai-service/tests/test_prompt_cache.py` 가 이를 회귀 테스트로 고정한다.

## 4. SPEC 대비 결정 사항

| 항목 | 내용 |
| --- | --- |
| `meeting_participant` 테이블 추가 | SPEC 3.1 DDL 에는 없지만 `participantUserIds` 저장과 대시보드의 `totalParticipantCount` 산출에 필요하다. (프로젝트 전체 멤버 ≠ 특정 회의 참석자) 그 외 8개 테이블은 DDL 그대로 매핑했다. |
| `interview_question` 테이블 추가 | 생성된 질문 캐시. 스트림 재요청 시 LLM 중복 호출을 막기 위한 확장 테이블이며 API 스펙은 바뀌지 않는다. |
| 프로젝트 `status` | `project` 테이블에 상태 컬럼이 없어 소속 회의 상태에서 파생한다. 회의가 1개 이상이고 전부 `COMPLETED` 면 `COMPLETED`, 그 외 `IN_PROGRESS`. |
| SSE 인증 | 브라우저 `EventSource` 는 커스텀 헤더를 못 보내므로 `Authorization` 헤더 외에 `?token={accessToken}` 쿼리 파라미터도 허용한다. |
| 로그아웃 | Stateless JWT 이므로 In-Memory 블랙리스트로 무효화한다. 운영 전환 시 Redis 교체 지점(`TokenBlacklist`). |
| 분석 작업 상태 | 업로드는 202 Accepted 로 즉시 응답하고 `AnalysisTaskRegistry`(In-Memory)가 taskId 상태를 보관한다. SPEC 에 조회 엔드포인트가 없어 API 는 추가하지 않았다. |
| 초대 이메일 | 이미 가입된 사용자만 즉시 멤버로 등록하고, 미가입자는 참여 코드로 합류한다. (메일 발송은 범위 밖) |

## 5. 검증 현황

| 항목 | 결과 |
| --- | --- |
| `./gradlew build` (bootJar 생성) | 통과 |
| `./gradlew test` (단위 10건) | 통과 |
| `pytest` (AI 서비스 12건) | 통과 |
| 질문 캐싱 실측 | 같은 질문 2회 요청 → FastAPI 호출 1회, 응답 바이트 단위 동일 |
| 캐시 무효화 실측 | 답변 제출 후 재요청 → 재생성 확인 |
| FastAPI SSE 실제 스트림 (`curl -N`) | `data: {"chunk": ...}` → `data: {"status":"DONE"}` 확인 |
| 통합 플로우 (PostgreSQL + 두 서버 기동) | 통과 — 회원가입 → 로그인 → 프로젝트/회의 생성 → SSE 중계 → 답변 저장 → 회의록 업로드 → 결과 조회 → 아이디어 보드(익명 마스킹 + AI 리스크 카드) |
| GPT-4o 실제 호출 | **미검증** — `OPENAI_API_KEY` 미설정으로 Mock 모드에서만 확인 |
