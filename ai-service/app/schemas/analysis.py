"""회의록 분석 요청/응답 스키마 (Pydantic v2)."""

from pydantic import BaseModel, Field


class ActionItem(BaseModel):
    """실행 항목."""

    assignee: str = Field(default="미정", description="담당자")
    task: str = Field(..., description="할 일")


class AnalysisResult(BaseModel):
    """SPEC 2.6 의 회의 결과 구조. LLM structured output 스키마로도 사용한다."""

    purpose: str = Field(default="", description="회의 목적 요약")
    mainDiscussions: list[str] = Field(default_factory=list, description="주요 논의 내용")
    decisions: list[str] = Field(default_factory=list, description="결정 사항")
    actionItems: list[ActionItem] = Field(default_factory=list, description="실행 항목")
    misunderstandings: list[str] = Field(
        default_factory=list,
        description="다르게 이해될 수 있는 부분(가짜합의 리스크)",
    )


class AnalysisMeta(BaseModel):
    """분석 입력 메타데이터 (multipart 폼 필드)."""

    meetingTitle: str = ""
    purpose: str = ""
    agendas: list[str] = Field(default_factory=list)
    participants: list[str] = Field(default_factory=list)
