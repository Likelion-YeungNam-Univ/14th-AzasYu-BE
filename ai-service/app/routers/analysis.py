"""회의록 분석 라우터 (PDF / DOCX / TXT 파싱 + 구조화 추출)."""

import logging

from fastapi import APIRouter, File, Form, HTTPException, UploadFile, status

from app.schemas.analysis import AnalysisResult
from app.services.llm_chain import analyze_transcript
from app.services.parser import (
    EmptyDocumentError,
    UnsupportedFileTypeError,
    normalize,
    parse_document,
)

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/ai/analysis", tags=["AI Analysis"])


@router.post(
    "/summarize",
    response_model=AnalysisResult,
    status_code=status.HTTP_200_OK,
    summary="회의록 분석 및 오해 리스크 추출",
    description=(
        "TXT/PDF/DOCX 파일 또는 원문 텍스트를 받아 목적·주요 논의·결정 사항·실행 항목과 "
        "'다르게 이해될 수 있는 부분(가짜합의 리스크)'을 구조화된 JSON 으로 반환한다."
    ),
)
async def summarize_meeting(
    file: UploadFile | None = File(default=None, description="회의록 파일 (TXT/PDF/DOCX)"),
    rawText: str | None = Form(default=None, description="회의록 원문 텍스트"),
    meetingTitle: str = Form(default="", description="회의 제목"),
    purpose: str = Form(default="", description="사전 등록된 회의 목적"),
    agendas: str = Form(default="", description="안건 목록 (줄바꿈 구분)"),
    participants: str = Form(default="", description="참석자 이름 (쉼표 구분)"),
) -> AnalysisResult:
    transcript = ""

    if file is not None and file.filename:
        content = await file.read()
        try:
            transcript = parse_document(file.filename, content)
        except UnsupportedFileTypeError as exc:
            raise HTTPException(status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc
        except EmptyDocumentError as exc:
            raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(exc)) from exc
        except Exception as exc:  # noqa: BLE001 - 손상된 파일 방어
            logger.exception("문서 파싱 실패: %s", file.filename)
            raise HTTPException(
                status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail=f"문서를 파싱하지 못했습니다: {exc}",
            ) from exc

    if rawText:
        transcript = f"{transcript}\n{normalize(rawText)}".strip() if transcript else normalize(rawText)

    if not transcript:
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST,
            detail="분석할 회의록이 없습니다. file 또는 rawText 중 하나는 필수입니다.",
        )

    agenda_list = [line.strip() for line in agendas.splitlines() if line.strip()]
    participant_list = [name.strip() for name in participants.split(",") if name.strip()]

    try:
        return await analyze_transcript(
            transcript=transcript,
            meeting_title=meetingTitle,
            purpose=purpose,
            agendas=agenda_list,
            participants=participant_list,
        )
    except Exception as exc:  # noqa: BLE001 - LLM 오류를 502 로 변환
        logger.exception("회의록 분석 실패")
        raise HTTPException(
            status.HTTP_502_BAD_GATEWAY, detail=f"AI 분석에 실패했습니다: {exc}"
        ) from exc
