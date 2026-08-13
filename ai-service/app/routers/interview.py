"""AI 사전 인터뷰 스트리밍 라우터."""

import json
import logging
from collections.abc import AsyncIterator

from fastapi import APIRouter, status
from fastapi.responses import StreamingResponse

from app.schemas.interview import InterviewStreamRequest
from app.services.llm_chain import astream_interview_question

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/ai/interview", tags=["AI Interview"])

SSE_HEADERS = {
    "Cache-Control": "no-cache",
    "Connection": "keep-alive",
    # Nginx 등 프록시의 버퍼링 방지 (타자기 효과 유지)
    "X-Accel-Buffering": "no",
}


def _sse(payload: dict) -> str:
    """SSE data 프레임으로 직렬화한다."""

    return f"data: {json.dumps(payload, ensure_ascii=False)}\n\n"


async def _event_generator(request: InterviewStreamRequest) -> AsyncIterator[str]:
    try:
        async for chunk in astream_interview_question(request):
            yield _sse({"chunk": chunk})
    except Exception as exc:  # noqa: BLE001 - 스트림 중단을 클라이언트에 알려야 한다
        logger.exception("인터뷰 질문 생성 실패")
        yield _sse({"status": "ERROR", "message": f"AI 질문 생성에 실패했습니다: {exc}"})
    finally:
        yield _sse({"status": "DONE", "questionNum": request.questionNum})


@router.post(
    "/stream",
    status_code=status.HTTP_200_OK,
    summary="사전 인터뷰 질문 SSE 스트리밍",
    description=(
        "회의 컨텍스트/이전 답변/직전 회의 오해 리스크를 바탕으로 다음 질문을 생성해 "
        "토큰 단위로 스트리밍한다. Spring Boot 가 이 스트림을 그대로 클라이언트에 중계한다."
    ),
    response_class=StreamingResponse,
)
async def stream_interview_question(request: InterviewStreamRequest) -> StreamingResponse:
    return StreamingResponse(
        _event_generator(request),
        media_type="text/event-stream",
        headers=SSE_HEADERS,
    )
