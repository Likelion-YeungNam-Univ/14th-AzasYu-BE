# Project Guidelines for Claude Code

## Overview
이 프로젝트는 SPEC.md 명세서를 기반으로 구축됩니다. 
작업을 시작하기 전 항상 `SPEC.md`를 읽고 정의된 아키텍처 및 DTO 규격을 준수하세요.

## Build & Test Commands by Module

### 1. Backend (Spring Boot)
- Working Directory: `./backend`
- Test: `./gradlew test`
- Build: `./gradlew build`
- Style: Java 17+, Spring Boot 3.x, Google Java Style 준수

### 2. Frontend (React / TypeScript)
- Working Directory: `./frontend`
- Test: `npm test`
- Build: `npm run build`
- Style: Any 타입 금지, Functional Component 사용

### 3. AI Service (Python / FastAPI)
- Working Directory: `./ai-service`
- Test: `pytest`
- Style: PEP8 준수, Type Hinting 필수, Pydantic v2 사용

## Safety Rules
- `/data/raw/` 경로의 원본 데이터 파일은 절대 수정/삭제하지 말 것.
- `SPEC.md`에 정의되지 않은 임의의 API 엔드포인트 변경 금지.
