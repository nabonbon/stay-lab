package com.staylab.reservation;

import com.staylab.common.BusinessException;
import com.staylab.common.ErrorCode;
import com.staylab.room.Room;
import com.staylab.room.RoomType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 예약 1건. 체크인~체크아웃 사이의 숙박일은 ReservationNight로 1박당 1행씩 저장한다.
 */
@Entity
@Table(name = "reservation", indexes = @Index(name = "idx_reservation_guest_phone", columnList = "guest_phone"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id")
    private RoomType roomType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(nullable = false, length = 30)
    private String guestName;

    @Column(nullable = false, length = 20)
    private String guestPhone;

    @Column(nullable = false)
    private LocalDate checkIn;

    @Column(nullable = false)
    private LocalDate checkOut;

    @Enumerated(EnumType.STRING) // ORDINAL(0, 1)은 enum 순서가 바뀌면 데이터가 깨지므로 문자열로 저장
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime cancelledAt;

    // 예약을 저장하면 숙박일도 같이 저장되고(cascade), 목록에서 빠진 숙박일은 삭제된다(orphanRemoval)
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationNight> nights = new ArrayList<>();

    private Reservation(RoomType roomType, Room room, String guestName, String guestPhone,
                        LocalDate checkIn, LocalDate checkOut) {
        this.roomType = roomType;
        this.room = room;
        this.guestName = guestName;
        this.guestPhone = guestPhone;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.status = ReservationStatus.CONFIRMED;
    }

    public static Reservation confirm(RoomType roomType, Room room, String guestName, String guestPhone,
                                      LocalDate checkIn, LocalDate checkOut) {
        Reservation reservation = new Reservation(roomType, room, guestName, guestPhone, checkIn, checkOut);
        checkIn.datesUntil(checkOut)
                .forEach(date -> reservation.nights.add(new ReservationNight(reservation, room, date)));
        return reservation;
    }

    /**
     * 취소. UPDATE 쿼리를 호출하지 않는다. 트랜잭션 커밋 시점에 변경 감지로 reservation UPDATE,
     * orphanRemoval로 reservation_night DELETE가 나가면서 해당 날짜의 재고가 다시 열린다.
     */
    public void cancel() {
        if (status == ReservationStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ALREADY_CANCELLED);
        }
        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.nights.clear();
    }

    public int getNightCount() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
