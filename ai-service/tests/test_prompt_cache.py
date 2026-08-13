"""프롬프트 캐싱 활성화 조건과 태스크별 모델 분리 검증."""

import tiktoken

from app.config import Settings
from app.services import prompt_templates as pt

# OpenAI 자동 프롬프트 캐싱 최소 길이. 이 값 미만이면 캐시가 아예 걸리지 않는다.
CACHE_MIN_TOKENS = 1024
ENCODING = tiktoken.get_encoding("o200k_base")  # GPT-4o / GPT-5.x 계열 토크나이저


def token_count(text: str) -> int:
    return len(ENCODING.encode(text))


def test_interview_system_prompt_exceeds_cache_threshold() -> None:
    """모든 인터뷰 호출이 공유하는 접두부가 캐시 임계값을 넘어야 한다."""
    count = token_count(pt.INTERVIEW_SYSTEM_PROMPT)
    assert count >= CACHE_MIN_TOKENS, (
        f"시스템 프롬프트가 {count} 토큰이라 캐시({CACHE_MIN_TOKENS} 토큰)가 걸리지 않습니다. "
        "few-shot 예시를 줄였다면 되돌리세요."
    )


def test_system_prompt_is_the_shared_prefix() -> None:
    """캐시는 '공통 접두부'에만 걸리므로, 회의별로 달라지는 내용이 시스템 프롬프트에 없어야 한다."""
    prompt = pt.INTERVIEW_SYSTEM_PROMPT
    for volatile in ("6차 기획 회의", "이지혜", "{", "}"):
        assert volatile not in prompt, f"시스템 프롬프트에 가변 값이 섞여 캐시가 깨집니다: {volatile}"


def test_few_shot_examples_are_present() -> None:
    """캐시 임계값을 채우려고 넣은 내용이 실제 few-shot 예시인지 확인한다."""
    prompt = pt.INTERVIEW_SYSTEM_PROMPT
    assert prompt.count("[예시") >= 5
    assert "나쁜 질문:" in prompt and "좋은 질문:" in prompt


def test_task_based_model_defaults_are_separated() -> None:
    """인터뷰(소형) / 분석(플래그십) 모델이 서로 다른 기본값을 가져야 한다."""
    settings = Settings(_env_file=None)
    assert settings.openai_model_interview == "gpt-4o-mini"
    assert settings.openai_model_analysis == "gpt-5.6-terra"
    assert settings.openai_model_interview != settings.openai_model_analysis


def test_models_are_overridable_by_env(monkeypatch) -> None:
    monkeypatch.setenv("OPENAI_MODEL_INTERVIEW", "gpt-4o")
    monkeypatch.setenv("OPENAI_MODEL_ANALYSIS", "gpt-5.6-sol")

    settings = Settings(_env_file=None)

    assert settings.openai_model_interview == "gpt-4o"
    assert settings.openai_model_analysis == "gpt-5.6-sol"
