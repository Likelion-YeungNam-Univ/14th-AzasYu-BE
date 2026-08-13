"""SyncBridge AI Microservice (FastAPI).

- POST /ai/interview/stream   : 사전 인터뷰 질문 SSE 스트리밍
- POST /ai/analysis/summarize : 회의록(TXT/PDF/DOCX) 분석 및 오해 리스크 추출
"""

import logging
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import get_settings
from app.routers import analysis, interview

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)

settings = get_settings()


@asynccontextmanager
async def lifespan(_: FastAPI) -> AsyncIterator[None]:
    if settings.is_llm_enabled:
        logger.info(
            "LLM 활성화: interview=%s, analysis=%s",
            settings.openai_model_interview,
            settings.openai_model_analysis,
        )
    else:
        logger.warning(
            "OPENAI_API_KEY 가 설정되지 않아 Mock 모드로 기동합니다. "
            "(.env 에 키를 넣으면 GPT-4o 로 전환됩니다)"
        )
    yield


app = FastAPI(
    lifespan=lifespan,
    title="SyncBridge AI Service",
    version="1.0.0",
    description="가짜합의 방지 AI 협업 플랫폼의 AI 마이크로서비스 (LangChain + GPT-4o)",
    docs_url="/docs",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allow_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(interview.router)
app.include_router(analysis.router)


@app.get("/health", tags=["System"], summary="헬스 체크")
async def health() -> dict[str, object]:
    return {
        "status": "UP",
        "llmEnabled": settings.is_llm_enabled,
        "interviewModel": settings.openai_model_interview if settings.is_llm_enabled else "mock",
        "analysisModel": settings.openai_model_analysis if settings.is_llm_enabled else "mock",
    }
