package com.staylab.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.staylab.common.BusinessException;
import com.staylab.common.ErrorCode;
import com.staylab.reservation.dto.ReservationRequest;
import com.staylab.reservation.dto.ReservationResponse;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * data.sql 기준: 2인실(id=1) 객실 101·102·103, 가족실(id=3) 객실 301 1개.
 * 각 테스트는 끝나면 롤백된다.
 */
@SpringBootTest
@Transactional
class ReservationServiceTest {

    private static final long DOUBLE_ROOM = 1L;
    private static final long FAMILY_ROOM = 3L;
    private static final LocalDate CHECK_IN = LocalDate.now().plusDays(30);

    @Autowired
    ReservationService reservationService;

    @Autowired
    ReservationNightRepository nightRepository;

    @Autowired
    EntityManager em;

    @Test
    @DisplayName("예약하면 빈 객실 중 번호가 가장 낮은 객실이 배정되고, 1박당 숙박일 행이 생긴다")
    void reserve_assignsFirstFreeRoom() {
        ReservationResponse response = reservationService.reserve(request(DOUBLE_ROOM, CHECK_IN, CHECK_IN.plusDays(2)));

        assertThat(response.roomNo()).isEqualTo("101");
        assertThat(response.nights()).isEqualTo(2);
        assertThat(response.status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(nightRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("같은 기간 두 번째 예약은 다른 객실로 배정된다")
    void reserve_sameDates_assignsDifferentRoom() {
        reservationService.reserve(request(DOUBLE_ROOM, CHECK_IN, CHECK_IN.plusDays(1)));
        ReservationResponse second = reservationService.reserve(request(DOUBLE_ROOM, CHECK_IN, CHECK_IN.plusDays(1)));

        assertThat(second.roomNo()).isEqualTo("102");
    }

    @Test
    @DisplayName("기간이 하루라도 겹치면 같은 객실을 배정하지 않는다")
    void reserve_overlappingDates_skipsOccupiedRoom() {
        reservationService.reserve(request(FAMILY_ROOM, CHECK_IN, CHECK_IN.plusDays(2)));

        assertThatThrownBy(() -> reservationService.reserve(request(FAMILY_ROOM, CHECK_IN.plusDays(1), CHECK_IN.plusDays(3))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SOLD_OUT);
    }

    @Test
    @DisplayName("체크아웃 날은 비어 있으므로, 앞 예약의 체크아웃 날짜에 체크인할 수 있다")
    void reserve_checkInOnPreviousCheckOut_isAllowed() {
        reservationService.reserve(request(FAMILY_ROOM, CHECK_IN, CHECK_IN.plusDays(2)));
        ReservationResponse next = reservationService.reserve(request(FAMILY_ROOM, CHECK_IN.plusDays(2), CHECK_IN.plusDays(3)));

        assertThat(next.roomNo()).isEqualTo("301");
    }

    @Test
    @DisplayName("취소하면 상태가 바뀌고 숙박일이 삭제되어 같은 기간을 다시 예약할 수 있다")
    void cancel_releasesNights() {
        ReservationResponse reserved = reservationService.reserve(request(FAMILY_ROOM, CHECK_IN, CHECK_IN.plusDays(1)));

        ReservationResponse cancelled = reservationService.cancel(reserved.reservationId());
        em.flush(); // 실제 운영에서는 커밋 시점에 일어나는 UPDATE·DELETE를 테스트 안에서 바로 실행
        em.clear(); // 1차 캐시를 비워서 다음 조회가 DB에서 다시 읽게 한다

        assertThat(cancelled.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservationService.findById(reserved.reservationId()).status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(nightRepository.count()).isZero();
        assertThat(reservationService.reserve(request(FAMILY_ROOM, CHECK_IN, CHECK_IN.plusDays(1))).roomNo()).isEqualTo("301");
    }

    @Test
    @DisplayName("이미 취소된 예약은 다시 취소할 수 없다")
    void cancel_twice_fails() {
        ReservationResponse reserved = reservationService.reserve(request(FAMILY_ROOM, CHECK_IN, CHECK_IN.plusDays(1)));
        reservationService.cancel(reserved.reservationId());

        assertThatThrownBy(() -> reservationService.cancel(reserved.reservationId()))
                .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_CANCELLED);
    }

    @Test
    @DisplayName("체크아웃이 체크인보다 빠르거나 최대 박수를 넘으면 예약할 수 없다")
    void reserve_invalidPeriod_fails() {
        assertThatThrownBy(() -> reservationService.reserve(request(DOUBLE_ROOM, CHECK_IN, CHECK_IN)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_STAY_PERIOD);
        assertThatThrownBy(() -> reservationService.reserve(request(DOUBLE_ROOM, CHECK_IN, CHECK_IN.plusDays(4))))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_STAY_PERIOD);
    }

    @Test
    @DisplayName("전화번호로 예약 목록을 조회한다")
    void findByGuestPhone() {
        reservationService.reserve(request(DOUBLE_ROOM, CHECK_IN, CHECK_IN.plusDays(1)));
        reservationService.reserve(request(DOUBLE_ROOM, CHECK_IN.plusDays(7), CHECK_IN.plusDays(8)));
        em.flush();
        em.clear();

        assertThat(reservationService.findByGuestPhone("010-1234-5678"))
                .extracting(ReservationResponse::checkIn)
                .containsExactly(CHECK_IN.plusDays(7), CHECK_IN);
    }

    private ReservationRequest request(long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        return new ReservationRequest(roomTypeId, "홍길동", "010-1234-5678", checkIn, checkOut);
    }
}
