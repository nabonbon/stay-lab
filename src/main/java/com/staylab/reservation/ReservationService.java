package com.staylab.reservation;

import com.staylab.common.BusinessException;
import com.staylab.common.ErrorCode;
import com.staylab.reservation.dto.ReservationRequest;
import com.staylab.reservation.dto.ReservationResponse;
import com.staylab.room.Room;
import com.staylab.room.RoomRepository;
import com.staylab.room.RoomType;
import com.staylab.room.RoomTypeRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    static final int MAX_NIGHTS = 3;

    private final ReservationRepository reservationRepository;
    private final ReservationNightRepository nightRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;

    /**
     * 예약 신청: 객실 타입과 기간을 받아 비어 있는 객실을 자동 배정한다.
     *
     * 주의: 1주차 버전은 락이 없다. "빈 객실 조회"와 "저장" 사이에 다른 요청이 끼어들면
     * 같은 객실이 두 번 배정될 수 있다. 2주차에 이 오버부킹을 테스트로 재현하고,
     * 3주차에 JVM 락 / DB 비관적 락 / 유니크 제약 / Redis 분산 락으로 고친다.
     */
    @Transactional
    public ReservationResponse reserve(ReservationRequest request) {
        validateStayPeriod(request.checkIn(), request.checkOut());

        RoomType roomType = roomTypeRepository.findById(request.roomTypeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_TYPE_NOT_FOUND));

        Room room = findFreeRoom(roomType.getId(), request.checkIn(), request.checkOut());

        Reservation reservation = Reservation.confirm(roomType, room,
                request.guestName(), request.guestPhone(), request.checkIn(), request.checkOut());
        reservationRepository.save(reservation); // cascade로 reservation_night도 함께 INSERT

        return ReservationResponse.from(reservation);
    }

    public ReservationResponse findById(Long reservationId) {
        return ReservationResponse.from(getDetail(reservationId));
    }

    public List<ReservationResponse> findByGuestPhone(String guestPhone) {
        return reservationRepository.findByGuestPhoneOrderByCheckInDesc(guestPhone).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @Transactional
    public ReservationResponse cancel(Long reservationId) {
        Reservation reservation = getDetail(reservationId);
        reservation.cancel(); // save() 호출 없음. 커밋 시 변경 감지로 반영된다
        return ReservationResponse.from(reservation);
    }

    private Reservation getDetail(Long reservationId) {
        return reservationRepository.findDetailById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
    }

    private Room findFreeRoom(Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        Set<Long> occupied = nightRepository.findOccupiedRoomIds(roomTypeId, checkIn, checkOut);
        return roomRepository.findByRoomTypeIdOrderByRoomNo(roomTypeId).stream()
                .filter(room -> !occupied.contains(room.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.SOLD_OUT));
    }

    private void validateStayPeriod(LocalDate checkIn, LocalDate checkOut) {
        if (!checkOut.isAfter(checkIn) || checkIn.plusDays(MAX_NIGHTS).isBefore(checkOut)) {
            throw new BusinessException(ErrorCode.INVALID_STAY_PERIOD, MAX_NIGHTS);
        }
    }
}
