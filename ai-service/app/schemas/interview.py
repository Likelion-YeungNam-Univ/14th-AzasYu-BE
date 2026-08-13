"""AI 사전 인터뷰 요청/응답 스키마 (Pydantic v2)."""

from pydantic import BaseModel, Field


class PreviousAnswer(BaseModel):
    """사용자가 이미 제출한 사전 인터뷰 답변."""

    questionNum: int = Field(..., ge=1, description="질문 번호")
    questionText: str = Field(..., description="질문 원문")
    answerText: str = Field(..., description="사용자 답변")


class InterviewStreamRequest(BaseModel):
    """Spring Boot 가 조립해서 넘겨주는 인터뷰 컨텍스트."""

    meetingId: int | None = Field(default=None, description="회의 ID")
    questionNum: int = Field(default=1, ge=1, le=10, description="생성할 질문 번호")
    totalQuestions: int = Field(default=6, ge=1, le=10, description="전체 질문 수")
    participantName: str = Field(default="참석자", description="답변자 이름")
    meetingTitle: str = Field(default="", description="회의 제목")
    purpose: str = Field(default="", description="회의 목적")
    agendas: list[str] = Field(default_factory=list, description="안건 목록")
    previousAnswers: list[PreviousAnswer] = Field(
        default_factory=list, description="이 사용자의 이전 답변 맥락"
    )
    previousMeetingMisunderstandings: list[str] = Field(
        default_factory=list, description="직전 회의에서 감지된 오해 리스크"
    )
