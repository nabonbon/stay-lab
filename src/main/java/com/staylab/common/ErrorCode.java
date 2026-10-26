package com.staylab.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_STAY_PERIOD(HttpStatus.BAD_REQUEST, "체크아웃은 체크인 다음 날 이후여야 하며, 최대 %d박까지 예약할 수 있습니다."),
    INVALID_SEARCH_PERIOD(HttpStatus.BAD_REQUEST, "조회 기간은 시작일부터 최대 %d일입니다."),
    ROOM_TYPE_NOT_FOUND(HttpStatus.NOT_FOUND, "객실 타입을 찾을 수 없습니다."),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."),
    SOLD_OUT(HttpStatus.CONFLICT, "선택한 기간에 남은 객실이 없습니다."),
    ALREADY_CANCELLED(HttpStatus.CONFLICT, "이미 취소된 예약입니다.");

    private final HttpStatus status;
    private final String message;
}
