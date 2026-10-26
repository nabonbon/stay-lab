package com.staylab.room;

import com.staylab.room.dto.RoomTypeResponse;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    // 타입마다 객실 수를 따로 세면 N+1이 되므로, JOIN + GROUP BY 한 번으로 DTO에 바로 담는다
    @Query("""
            select new com.staylab.room.dto.RoomTypeResponse(rt.id, rt.name, rt.capacity, count(r))
              from RoomType rt
              left join Room r on r.roomType = rt
             group by rt.id, rt.name, rt.capacity
             order by rt.id
            """)
    List<RoomTypeResponse> findAllWithRoomCount();
}
