package com.staylab.reservation.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ReservationRequest(
        @NotNull Long roomTypeId,
        @NotBlank @Size(max = 30) String guestName,
        @NotBlank @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "휴대폰 번호 형식이 아닙니다.") String guestPhone,
        @NotNull @FutureOrPresent LocalDate checkIn,
        @NotNull LocalDate checkOut
) {
}
