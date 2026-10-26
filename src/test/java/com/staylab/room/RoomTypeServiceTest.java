package com.staylab.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.staylab.common.ErrorCode;
import com.staylab.reservation.ReservationService;
import com.staylab.reservation.dto.ReservationRequest;
import com.staylab.room.dto.DailyAvailability;
import com.staylab.room.dto.RoomTypeResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RoomTypeServiceTest {

    private static final LocalDate FROM = LocalDate.now().plusDays(30);

    @Autowired
    RoomTypeService roomTypeService;

    @Autowired
    ReservationService reservationService;

    @Test
    @DisplayName("객실 타입 목록에 타입별 객실 수가 함께 나온다")
    void findAll_withRoomCount() {
        assertThat(roomTypeService.findAll())
                .extracting(RoomTypeResponse::name, RoomTypeResponse::totalRooms)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("2인실", 3L),
                        org.assertj.core.groups.Tuple.tuple("4인실", 2L),
                        org.assertj.core.groups.Tuple.tuple("가족실", 1L));
    }

    @Test
    @DisplayName("잔여 캘린더는 기간의 모든 날짜를 돌려주고, 예약된 날만 잔여 수가 줄어든다")
    void findAvailability() {
        reservationService.reserve(new ReservationRequest(1L, "홍길동", "010-1234-5678", FROM.plusDays(1), FROM.plusDays(3)));

        List<DailyAvailability> calendar = roomTypeService.findAvailability(1L, FROM, FROM.plusDays(3));

        assertThat(calendar).extracting(DailyAvailability::remainingRooms).containsExactly(3L, 2L, 2L, 3L);
    }

    @Test
    @DisplayName("조회 기간이 최대 일수를 넘으면 실패한다")
    void findAvailability_tooLongPeriod_fails() {
        assertThatThrownBy(() -> roomTypeService.findAvailability(1L, FROM, FROM.plusDays(RoomTypeService.MAX_SEARCH_DAYS)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_SEARCH_PERIOD);
    }
}
