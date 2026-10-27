# stay-lab: 휴양소 예약 동시성 실험실

실무에서 겪은 **예약 오버부킹 장애**를 직접 재현하고, 락 전략 4가지를 테스트와 부하 테스트로 비교하는 개인 프로젝트입니다.
실무는 MyBatis로 개발했고, 이 프로젝트는 JPA로 새로 작성했습니다. (회사 코드·데이터 미사용)

## 기술 스택

Java 21 · Spring Boot 3.5 · Spring Data JPA (Hibernate) · H2 / MySQL 8.4 · JUnit 5 · Gradle

## 진행 상황

| 주차 | 내용 | 상태 |
| --- | --- | --- |
| 1 | 도메인 설계(ERD), JPA 엔티티, 예약 API | 완료 |
| 2 | Testcontainers(MySQL) 테스트, 동시 예약 테스트로 오버부킹 재현, [직접 실습: 대기자 신청](docs/exercises/week2-waitlist/README.md) | 진행 중 |
| 3 | 락 전략 4종 비교: JVM 락 / DB 비관적 락 / 유니크 제약 / Redis 분산 락 | 예정 |
| 4 | Docker · GitHub Actions 배포, k6 부하 테스트, Grafana | 예정 |
| 5 | Outbox 패턴으로 알림 발송 재시도, 결과 정리 | 예정 |
| 6 | 페이지 임베드형 AI 채팅 위젯 (Claude API, 잔여 객실 조회 도구) | 예정 |

## 실행

```bash
# H2 메모리 DB (설치 없이 바로 실행)
./gradlew bootRun

# MySQL
docker compose up -d
./gradlew bootRun --args='--spring.profiles.active=mysql'

# 테스트
./gradlew test
```

## API

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/room-types` | 객실 타입 목록 (타입별 객실 수 포함) |
| GET | `/api/room-types/{id}/availability?from=2026-11-15&to=2026-11-30` | 날짜별 잔여 객실 캘린더 (최대 62일) |
| POST | `/api/reservations` | 예약 신청, 빈 객실 자동 배정 (최대 3박) |
| GET | `/api/reservations/{id}` | 예약 상세 |
| GET | `/api/reservations?phone=010-1234-5678` | 전화번호로 예약 목록 |
| POST | `/api/reservations/{id}/cancel` | 예약 취소 |

```bash
curl -X POST localhost:8080/api/reservations -H 'Content-Type: application/json' \
  -d '{"roomTypeId":1,"guestName":"홍길동","guestPhone":"010-1234-5678","checkIn":"2026-11-15","checkOut":"2026-11-17"}'
```

오류는 RFC 7807 ProblemDetail 형식에 `code`를 더해 돌려줍니다. (예: `409 SOLD_OUT`, `409 ALREADY_CANCELLED`)

## 설계

- [ERD와 설계 메모](docs/ERD.md)
- 1주차 버전의 예약 배정은 **락이 없습니다.** "빈 객실 조회 → 저장" 사이에 다른 요청이 끼어들면 같은 객실이 두 번 배정됩니다. 2주차에 이 문제를 테스트로 재현하는 것이 출발점입니다.
