package com.staylab.room;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

    // 메서드 이름만으로 쿼리 생성: where room_type_id = ? order by room_no
    List<Room> findByRoomTypeIdOrderByRoomNo(Long roomTypeId);

    long countByRoomTypeId(Long roomTypeId);
}
