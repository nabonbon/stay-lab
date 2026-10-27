package com.staylab.waitlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.staylab.common.ErrorCode;
import com.staylab.waitlist.dto.WaitlistRequest;
import com.staylab.waitlist.dto.WaitlistResponse;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class WaitlistServiceTest {

    private static final long FAMILY_ROOM = 3L;
    private static final LocalDate CHECK_IN = LocalDate.now().plusDays(30);

    @Autowired
    WaitlistService waitlistService;

    @Autowired
    EntityManager em;

    @Test
    @DisplayName("대기 신청하면 WAITING 상태와 대기 순번 1번을 돌려준다")
    void register() {
        WaitlistResponse response = waitlistService.register(request("010-1111-1111", CHECK_IN, CHECK_IN.plusDays(1)));

        assertThat(response.waitlistId()).isNotNull();
        assertThat(response.roomTypeName()).isEqualTo("가족실");
        assertThat(response.status()).isEqualTo(WaitlistStatus.WAITING);
        assertThat(response.position()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 객실 타입·기간의 대기자는 신청 순서대로 순번이 매겨진다")
    void register_positionIncreases() {
        waitlistService.register(request("010-1111-1111", CHECK_IN, CHECK_IN.plusDays(1)));
        WaitlistResponse second = waitlistService.register(request("010-2222-2222", CHECK_IN, CHECK_IN.plusDays(1)));
        WaitlistResponse otherDates = waitlistService.register(request("010-3333-3333", CHECK_IN.plusDays(5), CHECK_IN.plusDays(6)));

        assertThat(second.position()).isEqualTo(2);
        assertThat(otherDates.position()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 전화번호로 같은 객실 타입·기간에 두 번 대기할 수 없다")
    void register_duplicate_fails() {
        waitlistService.register(request("010-1111-1111", CHECK_IN, CHECK_IN.plusDays(1)));

        assertThatThrownBy(() -> waitlistService.register(request("010-1111-1111", CHECK_IN, CHECK_IN.plusDays(1))))
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_WAITLIST);
    }

    @Test
    @DisplayName("앞 사람이 취소하면 뒷사람의 순번이 당겨진다")
    void cancel_pullsPositionForward() {
        WaitlistResponse first = waitlistService.register(request("010-1111-1111", CHECK_IN, CHECK_IN.plusDays(1)));
        WaitlistResponse second = waitlistService.register(request("010-2222-2222", CHECK_IN, CHECK_IN.plusDays(1)));

        WaitlistResponse cancelled = waitlistService.cancel(first.waitlistId());
        em.flush();
        em.clear();

        assertThat(cancelled.status()).isEqualTo(WaitlistStatus.CANCELLED);
        assertThat(waitlistService.findById(first.waitlistId()).status()).isEqualTo(WaitlistStatus.CANCELLED);
        assertThat(waitlistService.findById(second.waitlistId()).position()).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 취소된 대기는 다시 취소할 수 없다")
    void cancel_twice_fails() {
        WaitlistResponse first = waitlistService.register(request("010-1111-1111", CHECK_IN, CHECK_IN.plusDays(1)));
        waitlistService.cancel(first.waitlistId());

        assertThatThrownBy(() -> waitlistService.cancel(first.waitlistId()))
                .extracting("errorCode").isEqualTo(ErrorCode.WAITLIST_NOT_WAITING);
    }

    @Test
    @DisplayName("없는 대기 번호를 조회하면 WAITLIST_NOT_FOUND")
    void findById_notFound() {
        assertThatThrownBy(() -> waitlistService.findById(9999L))
                .extracting("errorCode").isEqualTo(ErrorCode.WAITLIST_NOT_FOUND);
    }

    private WaitlistRequest request(String phone, LocalDate checkIn, LocalDate checkOut) {
        return new WaitlistRequest(FAMILY_ROOM, "홍길동", phone, checkIn, checkOut);
    }
}
