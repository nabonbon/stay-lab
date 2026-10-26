package com.staylab.room;

import com.staylab.common.BusinessException;
import com.staylab.common.ErrorCode;
import com.staylab.reservation.ReservationNightRepository;
import com.staylab.reservation.ReservationNightRepository.BookedCount;
import com.staylab.room.dto.DailyAvailability;
import com.staylab.room.dto.RoomTypeResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 조회 전용: 변경 감지용 스냅샷을 만들지 않아 가볍다
public class RoomTypeService {

    static final int MAX_SEARCH_DAYS = 62;

    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final ReservationNightRepository nightRepository;

    public List<RoomTypeResponse> findAll() {
        return roomTypeRepository.findAllWithRoomCount();
    }

    /**
     * 날짜별 잔여 객실 캘린더. 예약이 없는 날도 0건으로 채워서 돌려준다.
     */
    public List<DailyAvailability> findAvailability(Long roomTypeId, LocalDate from, LocalDate to) {
        if (to.isBefore(from) || from.plusDays(MAX_SEARCH_DAYS - 1).isBefore(to)) {
            throw new BusinessException(ErrorCode.INVALID_SEARCH_PERIOD, MAX_SEARCH_DAYS);
        }
        if (!roomTypeRepository.existsById(roomTypeId)) {
            throw new BusinessException(ErrorCode.ROOM_TYPE_NOT_FOUND);
        }

        long totalRooms = roomRepository.countByRoomTypeId(roomTypeId);
        Map<LocalDate, Long> bookedByDate = nightRepository.countBookedByDate(roomTypeId, from, to).stream()
                .collect(Collectors.toMap(BookedCount::getStayDate, BookedCount::getBooked));

        return from.datesUntil(to.plusDays(1))
                .map(date -> {
                    long booked = bookedByDate.getOrDefault(date, 0L);
                    return new DailyAvailability(date, totalRooms, booked, totalRooms - booked);
                })
                .toList();
    }
}
