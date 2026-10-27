# 2주차 직접 실습: 대기자 신청 만들기

1주차 코드를 읽기만 하는 대신, **엔티티 하나와 등록·조회·취소 API를 직접 만들어 보는 실습**입니다.
정답 코드는 저장소에 없고, 대신 통과해야 할 테스트 6개(`WaitlistServiceTest.java`)를 준비해 두었습니다.
테스트가 모두 초록색이 되면 완성입니다. 예상 시간은 3~4시간입니다.

## 왜 대기자인가

- 1주차의 핵심 개념을 한 번씩 다 써야 합니다: `@Entity`, `@ManyToOne(LAZY)`, enum 저장, 메서드 이름 쿼리, `@Query`, 변경 감지, DTO 변환.
- 실무 예약 시스템에도 있던 기능이라 요구사항이 머릿속에 이미 있습니다.
- 3주차 이후 "예약이 취소되면 1번 대기자를 자동으로 예약 확정"으로 이어지고, 이 승격 로직이 다시 동시성 문제를 만듭니다.

## 1. 요구사항

객실이 다 차서 예약하지 못한 손님이 **대기 신청**을 합니다.

| 기능 | API | 규칙 |
| --- | --- | --- |
| 대기 신청 | `POST /api/waitlists` | 객실 타입, 이름, 전화번호, 체크인, 체크아웃을 받는다. 기간 규칙은 예약과 같다(1~3박). 같은 전화번호로 같은 타입·같은 기간에 이미 대기 중이면 409 |
| 대기 조회 | `GET /api/waitlists/{id}` | 대기 순번(`position`)을 함께 돌려준다. 순번 = 같은 타입·같은 기간의 WAITING 중 내 앞 사람 수 + 1. 취소된 건은 0 |
| 대기 취소 | `POST /api/waitlists/{id}/cancel` | WAITING일 때만 취소 가능. 이미 취소된 건이면 409 |
| (선택) 내 대기 목록 | `GET /api/waitlists?phone=...` | 테스트는 없음. 여유가 있으면 |

이번 실습에서는 "실제로 만실일 때만 대기 가능" 규칙과 자동 승격은 **하지 않습니다.**

## 2. 만들 파일과 정해진 이름

테스트가 아래 이름을 그대로 쓰므로, **이름과 타입만은 맞춰 주세요.** 내부 구현은 자유입니다.

```
src/main/java/com/staylab/waitlist/
├── Waitlist.java            엔티티
├── WaitlistStatus.java      enum: WAITING, CANCELLED
├── WaitlistRepository.java
├── WaitlistService.java
├── WaitlistController.java
└── dto/
    ├── WaitlistRequest.java    record(Long roomTypeId, String guestName, String guestPhone, LocalDate checkIn, LocalDate checkOut)
    └── WaitlistResponse.java   record(Long waitlistId, String roomTypeName, String guestName,
                                       LocalDate checkIn, LocalDate checkOut, WaitlistStatus status, long position)
```

`WaitlistService`의 공개 메서드:

```java
WaitlistResponse register(WaitlistRequest request);
WaitlistResponse findById(Long waitlistId);
WaitlistResponse cancel(Long waitlistId);
```

`common/ErrorCode.java`에 추가할 코드 3개:

```java
WAITLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "대기 신청을 찾을 수 없습니다."),
DUPLICATE_WAITLIST(HttpStatus.CONFLICT, "같은 객실 타입과 기간으로 이미 대기 중입니다."),
WAITLIST_NOT_WAITING(HttpStatus.CONFLICT, "대기 중인 신청만 취소할 수 있습니다.");
```

## 3. 단계별 진행

각 단계가 끝날 때마다 커밋하세요. 커밋 메시지 예시를 함께 적었습니다.

### Step 0. 브랜치 만들기

```bash
cd ~/stay-lab
git checkout main && git pull
git checkout -b feature/waitlist
```

### Step 1. 엔티티와 enum

- 할 일: `WaitlistStatus`, `Waitlist` 작성. 필드는 id, 객실 타입(연관관계), 이름, 전화번호, 체크인, 체크아웃, 상태, 신청 시각.
- 참고: `reservation/Reservation.java`. 거의 같은 모양이지만 `room`과 `nights`가 없습니다.
- 확인: `./gradlew bootRun` 후 로그에 `create table waitlist (...)`가 나오면 성공. 컬럼 이름과 타입이 예상대로인지 보세요.
- 스스로 답해 보기: `roomType` 필드에 `fetch = LAZY`를 빼면 대기 조회 시 SQL이 어떻게 달라질까?
- 커밋: `대기자 엔티티 추가`

### Step 2. Repository

- 할 일: 두 가지 쿼리가 필요합니다.
  1. "같은 전화번호·타입·기간으로 WAITING인 대기가 있는가" (중복 체크)
  2. "같은 타입·기간의 WAITING 중 id가 나보다 작거나 같은 건수" (순번)
- 참고: 1번은 먼저 **메서드 이름 쿼리**로 시도해 보세요. 2번은 `ReservationNightRepository`의 `@Query`를 참고하세요.
- 확인: 앱이 에러 없이 뜨면 메서드 이름이 해석된 것입니다. (해석 실패하면 시작할 때 바로 에러가 납니다)
- 커밋: `대기자 Repository 추가`

### Step 3. DTO와 ErrorCode

- 할 일: `WaitlistRequest`(검증 어노테이션 포함), `WaitlistResponse`, ErrorCode 3개.
- 참고: `reservation/dto/`. `WaitlistResponse`에 엔티티와 순번을 받아 만드는 정적 메서드를 두면 편합니다.
- 커밋: `대기자 요청·응답 DTO와 오류 코드 추가`

### Step 4. Service

- 할 일: `register`, `findById`, `cancel`.
- 참고: `ReservationService`. 클래스에는 `readOnly = true`, 쓰기 메서드에만 `@Transactional`.
- 핵심: `cancel`에서 **save나 update를 호출하지 않고** 엔티티 메서드로 상태만 바꾸세요. 규칙("WAITING일 때만")도 엔티티 안에 두세요.
- 커밋: `대기 신청·조회·취소 서비스 구현`

### Step 5. 테스트 통과시키기

```bash
cp docs/exercises/week2-waitlist/WaitlistServiceTest.java src/test/java/com/staylab/waitlist/
./gradlew test
```

- 6개가 모두 통과할 때까지 고칩니다. 실패하면 `build/reports/tests/test/index.html`에서 어떤 단언이 왜 틀렸는지 보세요.
- 통과한 뒤, 실행된 SQL 로그를 보고 "취소 시 UPDATE가 언제 나가는지" 확인하세요. 테스트의 `em.flush()` 줄을 지우면 어떻게 될까요?
- 커밋: `대기자 서비스 테스트 추가`

### Step 6. Controller

- 할 일: 위 표의 API 3개(+ 선택 1개).
- 참고: `reservation/ReservationController.java`
- 확인: 앱을 띄우고 curl로 신청 → 조회 → 취소 → 다시 취소(409).

```bash
curl -X POST localhost:8080/api/waitlists -H 'Content-Type: application/json' \
  -d '{"roomTypeId":3,"guestName":"홍길동","guestPhone":"010-1234-5678","checkIn":"2026-12-01","checkOut":"2026-12-02"}'
curl localhost:8080/api/waitlists/1
curl -X POST localhost:8080/api/waitlists/1/cancel
```

- 커밋: `대기자 API 추가`

### Step 7. 올리고 리뷰 받기

```bash
git push -u origin feature/waitlist
```

GitHub에서 PR을 만들고 알려 주시면 코드 리뷰를 남기겠습니다. 리뷰는 정답 맞추기가 아니라 "JPA답게 썼는가"를 봅니다.

## 4. 막히면 펼쳐 보기

<details>
<summary>Step 2: 메서드 이름 쿼리가 "No property 'check' found" 에러를 낸다</summary>

`...AndCheckInAndCheckOut...`에서 Spring Data가 `CheckIn`의 `In`을 **IN 조건 키워드**로 읽어 버립니다. 필드 이름이 키워드로 끝나는 함정입니다.
해결은 `@Query`로 JPQL을 직접 쓰는 것입니다. `select count(w) > 0 from Waitlist w where ...`로 boolean을 돌려받을 수 있습니다.
메서드 이름 쿼리가 언제 한계에 부딪히는지 직접 겪어 보는 것이 이 단계의 목적입니다.
</details>

<details>
<summary>Step 2: JPQL에서 enum 값을 조건에 쓰는 법</summary>

파라미터로 넘기거나(`:status`), 전체 경로를 쓰면 됩니다: `w.status = com.staylab.waitlist.WaitlistStatus.WAITING`
</details>

<details>
<summary>Step 4: 순번 계산 쿼리에 무엇을 넘겨야 할지 모르겠다</summary>

저장(`save`)한 뒤에는 id가 생겨 있습니다(IDENTITY 전략이라 save 시점에 INSERT). 그 엔티티의 타입 id, 체크인, 체크아웃, id를 넘기면 됩니다.
</details>

<details>
<summary>Step 5: 취소 테스트에서 순번이 안 당겨진다</summary>

취소 메서드가 `@Transactional`(readOnly 아님)인지 확인하세요. 클래스에 붙은 `readOnly = true`가 적용되면 flush가 일어나지 않습니다.
</details>

## 5. 다 끝나고 스스로 답해 보기

1. MyBatis로 같은 기능을 만들었다면 파일이 몇 개, SQL이 몇 개 필요했을까?
2. 대기 조회 API에서 실행된 SQL은 몇 개였나? `roomType.getName()` 때문에 추가 SELECT가 나갔다면, 어떻게 1개로 줄일까?
3. 두 사람이 동시에 같은 전화번호로 대기 신청하면 중복 체크를 통과할 수 있을까? (3주차 주제와 같은 문제입니다)
