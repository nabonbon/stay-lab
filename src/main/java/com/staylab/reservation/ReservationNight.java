package com.staylab.reservation;

import com.staylab.room.Room;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 1박 단위 재고. "어떤 객실이 어느 날짜에 찼는가"를 행 하나로 표현한다.
 * (room_id, stay_date)에 유니크 제약을 걸면 DB가 중복 예약을 막는 최후 방어선이 되는데,
 * 3주차에 락 전략 중 하나로 비교하기 위해 지금은 일반 인덱스만 둔다.
 */
@Entity
@Table(name = "reservation_night",
        indexes = @Index(name = "idx_night_room_date", columnList = "room_id, stay_date"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationNight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(nullable = false)
    private LocalDate stayDate;

    ReservationNight(Reservation reservation, Room room, LocalDate stayDate) {
        this.reservation = reservation;
        this.room = room;
        this.stayDate = stayDate;
    }
}
