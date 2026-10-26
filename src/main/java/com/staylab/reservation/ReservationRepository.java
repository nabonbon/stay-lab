package com.staylab.reservation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // fetch join: 예약과 객실 타입·객실을 쿼리 1번으로 같이 가져온다
    @Query("""
            select r
              from Reservation r
              join fetch r.roomType
              join fetch r.room
             where r.id = :id
            """)
    Optional<Reservation> findDetailById(@Param("id") Long id);

    // @EntityGraph: 위와 같은 효과를 메서드 이름 쿼리에 어노테이션으로 붙인다. 빼면 목록 건수만큼 N+1이 생긴다
    @EntityGraph(attributePaths = {"roomType", "room"})
    List<Reservation> findByGuestPhoneOrderByCheckInDesc(String guestPhone);
}
