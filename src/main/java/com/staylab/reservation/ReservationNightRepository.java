package com.staylab.reservation;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationNightRepository extends JpaRepository<ReservationNight, Long> {

    /**
     * 기간 중 하루라도 찬 객실 id. n.room.roomType.id 처럼 점으로 따라가면 JPA가 room 조인을 만들어 준다.
     */
    @Query("""
            select distinct n.room.id
              from ReservationNight n
             where n.room.roomType.id = :roomTypeId
               and n.stayDate >= :checkIn
               and n.stayDate < :checkOut
            """)
    Set<Long> findOccupiedRoomIds(@Param("roomTypeId") Long roomTypeId,
                                  @Param("checkIn") LocalDate checkIn,
                                  @Param("checkOut") LocalDate checkOut);

    @Query("""
            select n.stayDate as stayDate, count(n) as booked
              from ReservationNight n
             where n.room.roomType.id = :roomTypeId
               and n.stayDate between :from and :to
             group by n.stayDate
            """)
    List<BookedCount> countBookedByDate(@Param("roomTypeId") Long roomTypeId,
                                        @Param("from") LocalDate from,
                                        @Param("to") LocalDate to);

    // 인터페이스 프로젝션: select 별칭(as stayDate, as booked)과 getter 이름이 맞으면 Spring Data가 구현체를 만든다
    interface BookedCount {
        LocalDate getStayDate();

        Long getBooked();
    }
}
