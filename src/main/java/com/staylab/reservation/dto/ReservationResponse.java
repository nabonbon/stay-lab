package com.staylab.reservation.dto;

import com.staylab.reservation.Reservation;
import com.staylab.reservation.ReservationStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long reservationId,
        String roomTypeName,
        String roomNo,
        String guestName,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        ReservationStatus status,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt
) {

    // 엔티티를 그대로 응답하지 않는다: LAZY 프록시 직렬화 오류, 내부 필드 노출, API 스펙이 엔티티에 묶이는 문제를 막기 위해
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getRoomType().getName(),
                r.getRoom().getRoomNo(),
                r.getGuestName(),
                r.getCheckIn(),
                r.getCheckOut(),
                r.getNightCount(),
                r.getStatus(),
                r.getCreatedAt(),
                r.getCancelledAt());
    }
}
