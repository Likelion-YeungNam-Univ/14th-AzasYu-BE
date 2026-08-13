"""AI 마이크로서비스 스모크 테스트 (OpenAI 키 없이 Mock 모드로 검증)."""

import json

from fastapi.testclient import TestClient

from app.main import app
from app.services.parser import UnsupportedFileTypeError, parse_document

client = TestClient(app)


def test_health() -> None:
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "UP"


def test_interview_stream_emits_chunks_and_done() -> None:
    response = client.post(
        "/ai/interview/stream",
        json={
            "meetingId": 100,
            "questionNum": 2,
            "totalQuestions": 6,
            "participantName": "이지혜",
            "meetingTitle": "6차 기획 회의",
            "purpose": "서비스 방향 설정",
            "agendas": ["1. 주요 기능 확정"],
            "previousAnswers": [],
            "previousMeetingMisunderstandings": [],
        },
    )
    assert response.status_code == 200
    assert response.headers["content-type"].startswith("text/event-stream")

    payloads = [
        json.loads(line[len("data: ") :])
        for line in response.text.splitlines()
        if line.startswith("data: ")
    ]
    assert any("chunk" in payload for payload in payloads)
    assert payloads[-1] == {"status": "DONE", "questionNum": 2}


def test_summarize_with_raw_text() -> None:
    response = client.post(
        "/ai/analysis/summarize",
        data={
            "rawText": (
                "이번 회의에서는 핵심 기능을 먼저 정하기로 했습니다.\n"
                "초기 버전은 MVP 중심으로 빠르게 만들기로 했습니다.\n"
                "타겟 사용자 조사는 다음 회의 전까지 정리합니다.\n"
                "경쟁 서비스 분석도 함께 진행합니다.\n"
            ),
            "meetingTitle": "6차 기획 회의",
            "purpose": "서비스 방향 설정",
            "agendas": "1. 주요 기능 확정\n2. 타겟 논의",
            "participants": "이지혜,박소민",
        },
    )
    assert response.status_code == 200

    body = response.json()
    assert set(body) == {
        "purpose",
        "mainDiscussions",
        "decisions",
        "actionItems",
        "misunderstandings",
    }
    assert body["misunderstandings"]


def test_summarize_requires_input() -> None:
    response = client.post("/ai/analysis/summarize", data={})
    assert response.status_code == 400


def test_summarize_rejects_unsupported_extension() -> None:
    response = client.post(
        "/ai/analysis/summarize",
        files={"file": ("note.hwp", b"content", "application/octet-stream")},
    )
    assert response.status_code == 400


def test_parse_txt_handles_cp949() -> None:
    text = parse_document("note.txt", "회의록 본문입니다.".encode("cp949"))
    assert "회의록" in text


def test_parse_document_rejects_unknown_type() -> None:
    try:
        parse_document("note.hwp", b"x")
    except UnsupportedFileTypeError:
        return
    raise AssertionError("UnsupportedFileTypeError 가 발생해야 합니다.")
