"""애플리케이션 설정 (Pydantic v2 Settings)."""

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """`.env` 파일 또는 환경변수로 주입되는 설정 값."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    openai_api_key: str = ""

    # --- 태스크별 모델 분리 ---
    # 인터뷰 질문 생성: 출력이 짧은 규칙 준수형 생성 → 소형 모델로 충분
    openai_model_interview: str = "gpt-4o-mini"
    # 회의록 분석: '말해지지 않은 전제'를 찾는 화용론적 추론 → 플래그십 필요
    openai_model_analysis: str = "gpt-5.6-terra"

    openai_temperature: float = 0.4

    port: int = 8000

    # Main Backend(Spring Boot) 및 프로토타이핑 클라이언트 오리진
    cors_allow_origins: str = "http://localhost:8080,http://localhost:3000,http://localhost:5173"

    # 스트리밍 타자기 효과용 청크 간 지연(초). 실제 LLM 스트림에는 적용하지 않는다.
    mock_chunk_delay_seconds: float = 0.05

    @property
    def allow_origins(self) -> list[str]:
        return [origin.strip() for origin in self.cors_allow_origins.split(",") if origin.strip()]

    @property
    def is_llm_enabled(self) -> bool:
        """OpenAI 키가 없으면 Mock 모드로 동작한다 (1인 프로토타이핑 검증용)."""
        return bool(self.openai_api_key and self.openai_api_key.startswith("sk-"))


@lru_cache
def get_settings() -> Settings:
    return Settings()
