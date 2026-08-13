# SPEC.md - SyncBridge (가짜합의 방지 AI 협업 플랫폼)

## 1. Project Overview & Scope
- **Active Scope:** Fullstack System Architecture & Core Microservice Specifications (Focus: Java Spring Boot Main BE + Python FastAPI AI Microservice + Prototyping Test Engine)
- **Problem & Solution:**
  - **Problem:** 조직 간/계급 간 오해로 인한 '가짜합의(Fake Agreement)' 및 아이디어 공유의 장벽.
  - **Solution:** AI 사전 인터뷰(개별 오해 가능성 추적)[cite: 1], 소프트 익명성 아이디어 보드[cite: 2], 회의록 AI 분석 리포트("다르게 이해될 수 있는 부분" 리스크 하이라이팅)[cite: 11]의 선순환 구조.
- **Tech Stack:**
  - **Main Web Backend:** Java 17+, Spring Boot 3.2+, Spring Data JPA, Spring Security, JWT, WebClient (Reactive), Springdoc OpenAPI 2.x (Swagger)
  - **AI Microservice:** Python 3.11+, FastAPI, LangChain, OpenAI GPT-4o API, PyPDF, python-docx, Uvicorn
  - **Database:** PostgreSQL 15+ (or MySQL 8.0+)
  - **Communication Protocols:**
    - Main BE ↔ Client: REST API (JSON) + SSE (Server-Sent Events) for AI Chat Streaming
    - Main BE ↔ AI Service: REST API & WebClient Reactive Stream Proxy
  - **Prototyping Strategy (1인 검증용):**
    - **Track A (Swagger UI):** 프론트엔드 팀(3명) 인수용 API 표준 명세 자동화 (`http://localhost:8080/swagger-ui.html`)
    - **Track B (Prototyping Client):** 단일 HTML/JS 파일(`index.html`)을 통한 SSE 스트리밍 & 파일 업로드 UX 1인 검증

```
[Client (Prototyping HTML / FE)]
    │
    ├── 1. Standard REST API ───────────► [Main BE: Spring Boot 3.2] ──► [PostgreSQL]
    ├── 2. SSE Chat Streaming ──────────► [Spring Boot WebClient] ────► [AI Microservice: FastAPI] ──► [OpenAI GPT-4o]
    └── 3. File Upload (TXT/PDF/DOCX) ──► [File Storage / Memory] ─────► [FastAPI Document Parser] ──► [Prompt Chain Engine]
```

---

## 2. API & Data Contracts

### 2.1 Authentication & User (`/api/v1/auth`)
1. **`POST /api/v1/auth/signup`** - 회원가입 (`Desktop - 8.pdf`)[cite: 15]
   - **Request Body:**
     ```json
     {
       "email": "haeeon03@naver.com",
       "password": "password123!",
       "passwordConfirm": "password123!",
       "name": "이지혜"
     }
     ```
   - **Response (201 Created):**
     ```json
     {
       "userId": 1,
       "email": "haeeon03@naver.com",
       "name": "이지혜"
     }
     ```
   - **Error (400 Bad Request):** 비밀번호 불일치 (`{"errorCode": "AUTH_001", "message": "비밀번호가 일치하지 않습니다."}`)
2. **`POST /api/v1/auth/login`** - 로그인 (`Desktop - 26, 27, 28.pdf`)[cite: 5, 6, 7]
   - **Request Body:**
     ```json
     {
       "email": "haeeon03@naver.com",
       "password": "password123!"
     }
     ```
   - **Response (200 OK):**
     ```json
     {
       "accessToken": "eyJhbGciOi...",
       "tokenType": "Bearer",
       "expiresIn": 86400
     }
     ```
   - **Error (401 Unauthorized):** 로그인 실패 (`{"errorCode": "AUTH_002", "message": "없는 아이디이거나 비밀번호가 일치하지 않습니다."}`)[cite: 7]
3. **`POST /api/v1/auth/logout`** - 로그아웃

### 2.2 Project Management (`/api/v1/projects`)
1. **`GET /api/v1/projects?status={ALL|IN_PROGRESS|COMPLETED}`** - 내 프로젝트 목록 조회 (`Desktop - 29.pdf`)[cite: 8]
   - **Response (200 OK):**
     ```json
     [
       {
         "projectId": 10,
         "name": "신규 서비스 기획",
         "description": "AI 기반 협업 서비스 기획 프로젝트",
         "color": "#4A90E2",
         "memberCount": 6,
         "representativeMemberName": "이지혜",
         "status": "IN_PROGRESS",
         "updatedAt": "2026-08-12T14:00:00"
       }
     ]
     ```
2. **`POST /api/v1/projects`** - 새 프로젝트 생성 (`Desktop - 30, 40.pdf`)[cite: 9, 14]
   - **Request Body:**
     ```json
     {
       "name": "신규 서비스 기획",
       "description": "가짜합의 방지를 위한 프로젝트",
       "color": "#4A90E2",
       "inviteEmails": ["park@naver.com", "lee@naver.com"]
     }
     ```
   - **Response (201 Created):**
     ```json
     {
       "projectId": 10,
       "name": "신규 서비스 기획",
       "joinCode": "A7K9-M2P4"
     }
     ```[cite: 14]
3. **`POST /api/v1/projects/join`** - 참여 코드로 프로젝트 참여 (`Desktop - 29.pdf`)[cite: 8]
   - **Request Body:**
     ```json
     {
       "joinCode": "A7K9-M2P4"
     }
     ```
   - **Response (200 OK):**
     ```json
     {
       "projectId": 10,
       "name": "신규 서비스 기획",
       "message": "프로젝트에 성공적으로 참여했습니다."
     }
     ```

### 2.3 Meeting Management (`/api/v1/meetings`)
1. **`POST /api/v1/projects/{projectId}/meetings`** - 회의 생성 (`Desktop - 31.pdf`)[cite: 10]
   - **Request Body:**
     ```json
     {
       "title": "6차 기획 회의",
       "purpose": "새로운 서비스의 방향을 설정하고, 핵심 기능과 초기 아이디어를 구체화합니다.",
       "agendas": [
         "1. 주요 기능 확정",
         "2. 타겟 논의",
         "3. 개발 일정 및 역할 분담"
       ],
       "meetingAt": "2026-08-12T14:00:00",
       "durationMinutes": 90,
       "participantUserIds": [1, 2, 3, 4, 5, 6]
     }
     ```
   - **Response (201 Created):**
     ```json
     {
       "meetingId": 100,
       "title": "6차 기획 회의",
       "status": "BEFORE_INTERVIEW"
     }
     ```
2. **`GET /api/v1/meetings/{meetingId}`** - 회의 상세 대시보드 (`Desktop - 11.pdf`)[cite: 3]
   - **Response (200 OK):**
     ```json
     {
       "meetingId": 100,
       "projectName": "신규 서비스 기획",
       "joinCode": "A7K9-M2P4",
       "title": "6차 기획 회의",
       "meetingAt": "2026-08-12T14:00:00",
       "completedInterviewCount": 4,
       "totalParticipantCount": 6,
       "status": "BEFORE_INTERVIEW"
     }
     ```

### 2.4 AI Pre-Interview (`/api/v1/meetings/{meetingId}/interview`)
1. **`GET /api/v1/meetings/{meetingId}/interview/stream?questionNum={1~6}` (SSE Streaming)** (`Desktop - 33.pdf`)[cite: 1]
   - **Description:** 이전 회의 결과 및 사전 답변 맥락 기반 질문을 타자기 효과로 실시간 스트리밍
   - **SSE Headers:** `Content-Type: text/event-stream`, `Cache-Control: no-cache`
   - **Data Payload Example:**
     ```text
     data: {"chunk": "이번에 "}
     data: {"chunk": "기획하고 있는 서비스는 "}
     data: {"chunk": "어떤 문제를 해결하기 위한 서비스인가요?"}
     data: {"status": "DONE", "questionNum": 2}
     ```
2. **`POST /api/v1/meetings/{meetingId}/interview/answer`** - 사전 인터뷰 답변 제출 (`Desktop - 33.pdf`)[cite: 1]
   - **Request Body:**
     ```json
     {
       "questionNum": 2,
       "questionText": "이번에 기획하고 있는 서비스는 어떤 문제를 해결하기 위한 서비스인가요?",
       "answerText": "정보가 너무 흩어져 있어서 불편한 문제를 해결하는 서비스라고 생각해요."
     }
     ```
   - **Response (200 OK):**
     ```json
     {
       "nextQuestionNum": 3,
       "isCompleted": false
     }
     ```

### 2.5 Anonymous Idea Board (`/api/v1/meetings/{meetingId}/ideas`)
1. **`GET /api/v1/meetings/{meetingId}/ideas`** - 아이디어 보드 조회 (**소프트 익명성 적용**) (`Desktop - 34.pdf`)[cite: 2]
   - **Response (200 OK):**
     ```json
     [
       {
         "ideaId": 501,
         "content": "추천 결과가 나온 이유를 함께 보여주면 좋겠어요.",
         "isAiGenerated": false,
         "authorName": "익명",
         "createdAt": "2026-08-12T11:00:00"
       },
       {
         "ideaId": 502,
         "content": "[AI 리스크 감지] 추천 기준의 투명성에 대해 팀원 간 시각차가 존재합니다.",
         "isAiGenerated": true,
         "authorName": "AI 챗봇",
         "createdAt": "2026-08-12T11:05:00"
       }
     ]
     ```
2. **`POST /api/v1/meetings/{meetingId}/ideas`** - 아이디어 작성 (`Desktop - 34.pdf`)[cite: 2]
   - **Request Body:**
     ```json
     {
       "content": "처음 사용하는 사람도 별도의 설명 없이 바로 이해할 수 있는 간단한 구조였으면 좋겠어요."
     }
     ```
   - **Response (201 Created):**
     ```json
     {
       "ideaId": 503,
       "status": "SUCCESS"
     }
     ```

### 2.6 Meeting Upload & AI Analysis (`/api/v1/meetings/{meetingId}/analysis`)
1. **`POST /api/v1/meetings/{meetingId}/upload`** - 회의 텍스트/파일 업로드 (`Desktop - 35.pdf`)[cite: 16]
   - **Content-Type:** `multipart/form-data`
   - **Form Fields:** `file` (MultipartFile - TXT/PDF/DOCX, Optional), `rawText` (String, Optional)
   - **Response (202 Accepted):**
     ```json
     {
       "taskId": "TASK_9921",
       "status": "PROCESSING",
       "message": "AI 분석이 시작되었습니다."
     }
     ```
2. **`GET /api/v1/meetings/{meetingId}/result`** - 전체 회의 결과 조회 (`Desktop - 36, 37.pdf`)[cite: 11, 12]
   - **Response (200 OK):**
     ```json
     {
       "meetingId": 100,
       "title": "6차 기획 회의 결과",
       "meetingAt": "2026.08.12 오후 2:00-4:00",
       "purpose": "새로운 서비스의 방향을 설정하고, 핵심 기능과 초기 아이디어를 구체화했습니다.",
       "mainDiscussions": [
         "사용자가 실제로 겪는 불편을 먼저 파악하고, 이를 바탕으로 서비스 방향을 설정하기로 했습니다.",
         "초기에는 많은 기능을 추가하기보다 핵심 기능 중심으로 MVP를 제작하기로 했습니다."
       ],
       "decisions": [
         "초기 버전은 핵심 기능에 집중하고, 이후 사용자 피드백을 바탕으로 기능을 확장합니다.",
         "다음 회의 전까지 경쟁 서비스 및 사용자 조사 결과를 정리합니다."
       ],
       "actionItems": [
         { "assignee": "이지혜", "task": "사용자 문제 및 핵심 타깃 정의" },
         { "assignee": "박소민", "task": "서비스 사용 흐름 설계" },
         { "assignee": "이승민", "task": "핵심 기능 구현 가능 여부 검토" }
       ],
       "misunderstandings": [
         "1. 핵심 기능의 범위를 어디까지로 볼 것인지에 대한 기준이 명확하지 않습니다.",
         "2. 테스트 대상이 누구인지, 몇 명을 대상으로 진행할 것인지 정해지지 않았습니다.",
         "3. 어떤 서비스를 경쟁 대상으로 보고 차별화할 것인지 명확하지 않습니다."
       ]
     }
     ```

---

## 3. Data Model & DB Schema

### 3.1 DDL (PostgreSQL Reference)

```sql
-- 1. USER
CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. PROJECT
CREATE TABLE project (
    project_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    color_code VARCHAR(10) NOT NULL DEFAULT '#4A90E2',
    join_code VARCHAR(20) UNIQUE NOT NULL,
    created_by BIGINT REFERENCES users(user_id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. PROJECT MEMBER
CREATE TABLE project_member (
    member_id BIGSERIAL PRIMARY KEY,
    project_id BIGINT REFERENCES project(project_id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_project_user UNIQUE (project_id, user_id)
);

-- 4. MEETING
CREATE TABLE meeting (
    meeting_id BIGSERIAL PRIMARY KEY,
    project_id BIGINT REFERENCES project(project_id) ON DELETE CASCADE,
    title VARCHAR(100) NOT NULL,
    purpose TEXT NOT NULL,
    meeting_at TIMESTAMP NOT NULL,
    duration_minutes INT DEFAULT 60,
    status VARCHAR(20) DEFAULT 'BEFORE_INTERVIEW', -- BEFORE_INTERVIEW, IN_PROGRESS, COMPLETED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. MEETING AGENDA
CREATE TABLE meeting_agenda (
    agenda_id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT REFERENCES meeting(meeting_id) ON DELETE CASCADE,
    content VARCHAR(255) NOT NULL,
    order_index INT NOT NULL
);

-- 6. PRE INTERVIEW ANSWER
CREATE TABLE pre_interview_answer (
    answer_id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT REFERENCES meeting(meeting_id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(user_id),
    question_num INT NOT NULL,
    question_text TEXT NOT NULL,
    answer_text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. IDEA CARD (Soft Anonymity: DB holds user_id, DTO masks as "익명")
CREATE TABLE idea_card (
    idea_id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT REFERENCES meeting(meeting_id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(user_id), -- DB 레벨 저장 유지 (개인 성과 반영 대비)
    content TEXT NOT NULL,
    is_ai_generated BOOLEAN DEFAULT FALSE,
    is_revealed BOOLEAN DEFAULT FALSE, -- 공개 전환 플래그
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. MEETING RESULT
CREATE TABLE meeting_result (
    result_id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT UNIQUE REFERENCES meeting(meeting_id) ON DELETE CASCADE,
    purpose_summary TEXT,
    main_discussions JSONB,  -- Array of strings
    decisions JSONB,         -- Array of strings
    action_items JSONB,      -- Array of {assignee, task}
    misunderstandings JSONB, -- Array of misunderstanding risk strings
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 4. Module & Component Breakdown

### 4.1 Java Spring Boot Main Backend
```text
src/main/java/com/syncbridge/app/
├── global/
│   ├── config/
│   │   ├── SecurityConfig.java (Spring Security & JWT)
│   │   ├── SwaggerConfig.java (Springdoc OpenAPI 2.x)
│   │   └── WebClientConfig.java (Reactive WebClient for FastAPI Communication)
│   ├── security/
│   │   ├── JwtTokenProvider.java
│   │   └── JwtAuthenticationFilter.java
│   └── error/
│       ├── GlobalExceptionHandler.java
│       └── CustomException.java
├── domain/
│   ├── auth/ (AuthController, AuthService, UserRepository, User Entity, Dtos)
│   ├── project/ (ProjectController, ProjectService, ProjectRepository, JoinCodeGenerator)
│   ├── meeting/ (MeetingController, MeetingService, Meeting Entity, Agenda Entity)
│   ├── interview/
│   │   ├── InterviewController.java (SSE Stream Handler)
│   │   └── InterviewService.java (Calls FastAPI via WebClient)
│   ├── ideaboard/
│   │   ├── IdeaController.java
│   │   └── IdeaService.java (Soft Anonymity: Maps user_id to "익명" in DTO)
│   └── result/
│       ├── MeetingResultController.java
│       ├── MeetingResultService.java
│       └── FileStorageService.java (Handles Multipart PDF/DOCX/TXT)
```

### 4.2 Python FastAPI AI Microservice
```text
ai-service/
├── app/
│   ├── main.py (FastAPI Setup & CORS)
│   ├── config.py (OpenAI API Key & Settings)
│   ├── routers/
│   │   ├── interview.py (POST /ai/interview/stream - StreamingResponse SSE)
│   │   └── analysis.py (POST /ai/analysis/summarize - Text/Doc Prompt Pipeline)
│   ├── services/
│   │   ├── parser.py (PyPDF, python-docx, text parser)
│   │   ├── prompt_templates.py (Few-shot prompts for catching misunderstandings)
│   │   └── llm_chain.py (LangChain GPT-4o chain)
│   └── schemas/ (Pydantic Models for requests/responses)
├── requirements.txt
└── Dockerfile
```

### 4.3 Prototyping Test Client (`prototyping/index.html`)
* **목적:** 1인 프로토타이핑 검증용 단일 HTML 파일 (SSE 스트리밍 대화 UI & 파일 업로드 파이프라인 테스트)

```html
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <title>SyncBridge Prototyping Tester</title>
  <style>
    body { font-family: sans-serif; padding: 20px; max-width: 800px; margin: 0 auto; }
    #chat-box { border: 1px solid #ccc; height: 300px; overflow-y: scroll; padding: 10px; margin-bottom: 10px; }
    .ai-msg { color: #0066cc; margin: 5px 0; }
    .user-msg { color: #222; font-weight: bold; margin: 5px 0; text-align: right; }
  </style>
</head>
<body>
  <h2>1. AI 사전 인터뷰 SSE 스트리밍 테스트</h2>
  <div id="chat-box"></div>
  <button onclick="startInterviewStream()">AI 질문 받기 (SSE Stream)</button>

  <h2>2. 회의록 파일 업로드 및 AI 분석 테스트</h2>
  <input type="file" id="fileInput" accept=".txt,.pdf,.docx">
  <button onclick="uploadMeetingFile()">파일 업로드 및 분석 요청</button>

  <script>
    function startInterviewStream() {
      const chatBox = document.getElementById('chat-box');
      const aiDiv = document.createElement('div');
      aiDiv.className = 'ai-msg';
      aiDiv.innerText = 'AI: ';
      chatBox.appendChild(aiDiv);

      // Spring Boot SSE Endpoint 호출
      const eventSource = new EventSource('/api/v1/meetings/100/interview/stream?questionNum=2');
      eventSource.onmessage = (event) => {
        const data = JSON.parse(event.data);
        if (data.chunk) {
          aiDiv.innerText += data.chunk;
          chatBox.scrollTop = chatBox.scrollHeight;
        }
        if (data.status === 'DONE') {
          eventSource.close();
        }
      };
    }

    async function uploadMeetingFile() {
      const fileInput = document.getElementById('fileInput');
      const formData = new FormData();
      formData.append('file', fileInput.files[0]);

      const res = await fetch('/api/v1/meetings/100/upload', { method: 'POST', body: formData });
      const result = await res.json();
      alert('업로드 결과: ' + JSON.stringify(result));
    }
  </script>
</body>
</html>
```

---

## 5. Environment & Commands (`CLAUDE.md` Reference)

### 5.1 Main Backend (Spring Boot)
- **Environment Variables (`application.yml`):**
  ```yaml
  spring:
    datasource:
      url: jdbc:postgresql://localhost:5432/syncbridge
      username: postgres
      password: secretpassword
    jpa:
      hibernate:
        ddl-auto: update
      show-sql: true
  jwt:
    secret: c3luY2JyaWRnZS1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tZ2VuZXJhdGlvbg==
  ai-service:
    url: http://localhost:8000
  ```
- **Commands:**
  - Build: `./gradlew build -x test`
  - Run App: `./gradlew bootRun`
  - Swagger UI Check: Open `http://localhost:8080/swagger-ui.html`

### 5.2 AI Microservice (FastAPI)
- **Environment Variables (`.env`):**
  ```env
  OPENAI_API_KEY=sk-proj-YOUR_OPENAI_KEY_HERE
  PORT=8000
  ```
- **Commands:**
  - Install dependencies: `pip install -r requirements.txt`
  - Run Server: `uvicorn app.main:app --reload --port 8000`
