# 모아책 — 전자책 통합 챗 검색

## 다른 사람에게 공개하기

이 프로젝트는 Docker와 Render Blueprint(`render.yaml`)로 배포할 수 있습니다.

1. 프로젝트를 GitHub 또는 GitLab 저장소에 올립니다.
2. Render에서 **New > Blueprint**를 선택하고 저장소를 연결합니다.
3. `render.yaml`을 확인한 뒤 배포합니다.

배포가 끝나면 `https://ebook-chat-search-....onrender.com` 형태의 공개 주소가 생성됩니다.
무료 인스턴스는 일정 시간 사용하지 않으면 정지되며, 첫 접속 때 다시 켜지는 시간이 걸릴 수 있습니다.

부산·서울 5개 전자도서관의 전자책을 자연어로 한 번에 검색하는 Spring Boot 프로젝트입니다.
Dropshop의 Java 17 / Spring Boot 구성과 도메인 단위 책임 분리 방식을 참고했습니다.

## 도메인 중심 3계층 아키텍처

```text
src/main/java/com/example/ebooksearch
├─ domain
│  └─ search
│     ├─ controller
│     ├─ dto
│     ├─ entity
│     ├─ repository
│     └─ service
└─ infrastructure
   └─ http
```

- `controller`: HTTP 요청 검증과 API 응답
- `dto`: 검색 요청/응답 전송 객체
- `entity`: `Book`, `LibrarySource`, `SearchCommand` 등 핵심 모델
- `repository`: 도서관별 검색 어댑터와 저장소 인터페이스
- `service`: 자연어 검색어 추출, 병렬 검색 조정 및 결과 통합

## 현재 연동 범위

- 결과 통합 수집: 부산시립시민도서관, 부산광역시 전자도서관, 부산 북구 전자도서관,
  부산강서전자도서관, 서울시립대 전자도서관

사이트가 외부 수집을 막거나 POST 검색만 제공하면 전체 요청을 실패시키지 않고 공식 검색 링크를 반환합니다.

## 실행

```powershell
.\gradlew.bat bootRun
```

브라우저에서 `http://localhost:8080`을 엽니다.

## API

`POST /api/v1/search/chat`

```json
{
  "message": "대출 가능한 AI 입문서 찾아줘",
  "libraryIds": ["simin", "busan-elib", "bukgu-ebook", "gangseo", "uos"]
}
```

도서관 원문 HTML 구조가 변경될 수 있으므로 운영 시 어댑터별 모니터링과 이용약관·robots 정책 검토가 필요합니다.
