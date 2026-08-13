"""LangChain + GPT-4o 체인 (인터뷰 질문 스트리밍 / 회의록 구조화 분석).

OpenAI API 키가 설정되지 않은 경우 자동으로 Mock 모드로 동작한다.
1인 프로토타이핑 단계에서 키 없이도 SSE 스트리밍/업로드 파이프라인을 검증할 수 있게 하기 위함이다.
"""

import asyncio
import logging
from collections.abc import AsyncIterator

from app.config import get_settings
from app.schemas.analysis import ActionItem, AnalysisResult
from app.schemas.interview import InterviewStreamRequest
from app.services import prompt_templates

logger = logging.getLogger(__name__)

# Mock 모드에서 사용할 질문 풀 (SPEC 2.4 예시 기반)
_MOCK_QUESTIONS = [
    "이번에 기획하고 있는 서비스는 어떤 문제를 해결하기 위한 서비스인가요?",
    "말씀하신 '핵심 기능'은 구체적으로 어디까지 포함된다고 생각하시나요?",
    "이 서비스를 가장 먼저 써야 할 사용자는 누구라고 보시나요?",
    "지금 논의 중인 일정 안에서 반드시 끝나야 하는 일은 무엇인가요?",
    "다른 팀원과 다르게 이해하고 있을 수도 있다고 느끼신 부분이 있나요?",
    "이번 회의가 끝났을 때 무엇이 정해져 있어야 성공한 회의라고 보시나요?",
]


def _get_chat_model(model: str, streaming: bool = False):
    """지정한 모델의 ChatOpenAI 인스턴스를 생성한다. (키가 없으면 None)

    태스크마다 요구되는 추론 난이도가 다르므로 호출부에서 모델을 명시한다.
    """

    settings = get_settings()
    if not settings.is_llm_enabled:
        return None

    from langchain_openai import ChatOpenAI

    return ChatOpenAI(
        model=model,
        temperature=settings.openai_temperature,
        streaming=streaming,
        api_key=settings.openai_api_key,
    )


# --------------------------------------------------------------------------------------
# 1. 사전 인터뷰 질문 스트리밍
# --------------------------------------------------------------------------------------


async def astream_interview_question(request: InterviewStreamRequest) -> AsyncIterator[str]:
    """인터뷰 질문을 토큰 단위로 스트리밍한다."""

    settings = get_settings()
    llm = _get_chat_model(settings.openai_model_interview, streaming=True)
    if llm is None:
        logger.warning("OPENAI_API_KEY 가 없어 Mock 질문을 스트리밍합니다.")
        async for chunk in _astream_mock_question(request):
            yield chunk
        return

    logger.info("인터뷰 질문 생성: model=%s, questionNum=%d", settings.openai_model_interview, request.questionNum)

    # 캐시 히트를 위해 공통 접두부(시스템 프롬프트)를 항상 먼저 배치한다.
    messages = [
        ("system", prompt_templates.INTERVIEW_SYSTEM_PROMPT),
        ("human", prompt_templates.build_interview_user_prompt(request)),
    ]

    async for chunk in llm.astream(messages):
        text = chunk.content
        if isinstance(text, list):  # 멀티모달 응답 방어
            text = "".join(part.get("text", "") for part in text if isinstance(part, dict))
        if text:
            yield text


async def _astream_mock_question(request: InterviewStreamRequest) -> AsyncIterator[str]:
    """키 없이도 타자기 효과를 검증할 수 있는 Mock 스트림."""

    settings = get_settings()
    index = (request.questionNum - 1) % len(_MOCK_QUESTIONS)
    question = _MOCK_QUESTIONS[index]

    chunk_size = 6
    for start in range(0, len(question), chunk_size):
        await asyncio.sleep(settings.mock_chunk_delay_seconds)
        yield question[start : start + chunk_size]


# --------------------------------------------------------------------------------------
# 2. 회의록 구조화 분석
# --------------------------------------------------------------------------------------


async def analyze_transcript(
    transcript: str,
    meeting_title: str = "",
    purpose: str = "",
    agendas: list[str] | None = None,
    participants: list[str] | None = None,
) -> AnalysisResult:
    """회의록을 분석해 SPEC 2.6 구조의 결과를 반환한다."""

    settings = get_settings()
    llm = _get_chat_model(settings.openai_model_analysis)
    if llm is None:
        logger.warning("OPENAI_API_KEY 가 없어 Mock 분석 결과를 반환합니다.")
        return _mock_analysis(transcript, purpose, participants)

    logger.info("회의록 분석: model=%s, 원문 %d자", settings.openai_model_analysis, len(transcript))

    structured_llm = llm.with_structured_output(AnalysisResult)
    messages = [
        ("system", prompt_templates.ANALYSIS_SYSTEM_PROMPT),
        (
            "human",
            prompt_templates.build_analysis_user_prompt(
                transcript=transcript,
                meeting_title=meeting_title,
                purpose=purpose,
                agendas=agendas,
                participants=participants,
            ),
        ),
    ]

    result = await structured_llm.ainvoke(messages)
    if isinstance(result, AnalysisResult):
        return result
    # with_structured_output 이 dict 를 반환하는 구현체 대응
    return AnalysisResult.model_validate(result)


def _mock_analysis(
    transcript: str, purpose: str, participants: list[str] | None
) -> AnalysisResult:
    """키가 없을 때 파이프라인 검증용으로 회의록에서 규칙 기반 결과를 만든다."""

    lines = [line for line in transcript.splitlines() if len(line) > 5]
    names = participants or []

    ambiguous_terms = ["핵심 기능", "MVP", "타겟", "초기 버전", "개선", "빠르게", "나중에", "일부"]
    detected = [term for term in ambiguous_terms if term in transcript]

    misunderstandings = [
        f"{index}. '{term}'의 범위와 기준이 참석자마다 다르게 이해될 수 있습니다."
        for index, term in enumerate(detected[:3], start=1)
    ] or ["1. (Mock) 회의록에서 모호한 합의 표현이 감지되지 않았습니다."]

    return AnalysisResult(
        purpose=purpose or (lines[0] if lines else "(Mock) 회의 목적을 확인할 수 없습니다."),
        mainDiscussions=lines[:3] or ["(Mock) 주요 논의 내용을 추출하지 못했습니다."],
        decisions=lines[3:5] or ["(Mock) 확정된 결정 사항이 확인되지 않았습니다."],
        actionItems=[
            ActionItem(assignee=name, task="(Mock) 후속 확인 필요") for name in names[:3]
        ]
        or [ActionItem(assignee="미정", task="(Mock) 담당자 지정 필요")],
        misunderstandings=misunderstandings,
    )
