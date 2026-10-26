# 1주차 학습 가이드: MyBatis 개발자가 읽는 순서

이번 주 코드는 "MyBatis라면 Mapper XML에 썼을 것"이 JPA에서는 어디로 갔는지 보는 것이 목표입니다.
실행한 뒤 콘솔에 찍히는 SQL을 같이 보면서 읽으세요. (`application.yml`에서 SQL·바인딩 값 로그를 켜 두었습니다)

## 1. 읽는 순서

| 순서 | 파일 | 볼 것 | MyBatis라면 |
| --- | --- | --- | --- |
| 1 | `room/RoomType`, `room/Room` | `@Entity`, `@ManyToOne(LAZY)` | 테이블 DDL + VO + resultMap |
| 2 | `reservation/Reservation` | `confirm()`으로 숙박일 생성, `cancel()`에 UPDATE 없음 | 서비스에서 INSERT·UPDATE 호출 |
| 3 | `reservation/ReservationNight` | 1박 1행 재고, 인덱스 | 재고 테이블 |
| 4 | `*Repository` | 메서드 이름 쿼리, JPQL `@Query`, fetch join, `@EntityGraph`, 프로젝션 | Mapper 인터페이스 + XML |
| 5 | `reservation/ReservationService` | `@Transactional`, 빈 객실 배정 | 같음 |
| 6 | `src/test/...` | `em.flush()`·`em.clear()`가 왜 필요한지 | 없음 |

## 2. 직접 확인한 SQL (취소 API 1번 호출)

```text
select ... from reservation join room_type join room where id=?   ← fetch join: 1번에 같이 조회
select ... from reservation_night where reservation_id=?          ← nights.clear() 하려고 컬렉션 로딩
update reservation set cancelled_at=?, check_in=?, check_out=?, ... where id=?   ← 변경 감지
delete from reservation_night where id=?                           ← orphanRemoval, 1박마다 1번
delete from reservation_night where id=?
```

여기서 JPA의 특징 두 가지가 그대로 보입니다.

- **UPDATE가 전체 컬럼을 보낸다.** 바뀐 건 `status`, `cancelled_at`뿐인데 모든 컬럼이 SET에 들어갑니다. Hibernate의 기본 동작이고, 바뀐 컬럼만 보내려면 `@DynamicUpdate`를 붙입니다.
- **DELETE가 행마다 나간다.** 3박이면 3번입니다. 지금 규모에서는 문제가 아니지만, 많아지면 `delete from ReservationNight n where n.reservation.id = :id` 같은 벌크 JPQL로 바꿉니다. 이때 벌크 쿼리는 영속성 컨텍스트를 거치지 않는다는 점을 같이 공부하면 좋습니다.

## 3. 해 볼 실습 (30분)

1. `./gradlew bootRun` 후 README의 curl로 예약 → 취소를 호출하고 위 SQL이 나오는지 확인
2. `ReservationRepository`의 `@EntityGraph`를 지우고 같은 전화번호로 예약 3건을 만든 뒤 목록 조회 → SELECT 개수 세기
   (다만 `default_batch_fetch_size: 100` 때문에 건건이가 아니라 `IN (...)`으로 묶여 나옵니다. 이것도 N+1 해결책 중 하나라는 걸 확인하는 실습입니다. 설정까지 지우면 진짜 N+1이 보입니다)
3. `ReservationService.cancel()` 위의 `@Transactional`만 지워 보기 → 클래스에 붙은 `readOnly = true`가 적용되어 flush가 일어나지 않고, 응답은 CANCELLED인데 DB는 그대로인 것 확인

## 4. 면접에서 바로 쓸 수 있는 질문과 답

| 질문 | 이 코드의 답 |
| --- | --- |
| 엔티티를 API 응답으로 바로 내보내면? | LAZY 프록시 직렬화 오류, 내부 필드 노출, 엔티티 변경이 API 스펙 변경이 됨. 그래서 `ReservationResponse` DTO 사용 |
| `open-in-view`를 왜 껐나? | 켜 두면 응답이 끝날 때까지 DB 커넥션을 잡고 있음. 예약 오픈처럼 몰리는 서비스에서 커넥션 고갈 원인 |
| enum을 왜 STRING으로? | ORDINAL은 순서 번호라 enum 순서가 바뀌면 기존 데이터 의미가 바뀜 |
| 조회 서비스에 `readOnly = true`는 왜? | 변경 감지용 스냅샷을 만들지 않고 flush도 생략해 가벼움 |

## 5. 2주차 예고

지금 `ReservationService.reserve()`는 "빈 객실 조회 → 저장" 사이에 락이 없습니다.
2주차에는 MySQL(Testcontainers)에서 100개 스레드가 같은 객실 타입·날짜로 동시에 예약하는 테스트를 만들어,
**실무에서 겪은 오버부킹을 실패하는 테스트로 재현**합니다. Testcontainers는 Docker가 필요하니 PC에 Docker Desktop을 설치해 두세요.
