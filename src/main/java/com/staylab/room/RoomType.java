package com.staylab.room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 객실 타입 (2인실, 4인실, 가족실). 사용자는 타입만 고르고 실제 객실은 서버가 배정한다.
 */
@Entity
@Table(name = "room_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA는 기본 생성자가 필요하지만, 외부에서 빈 객체를 만들지 못하게 막는다
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private int capacity;

    public RoomType(String name, int capacity) {
        this.name = name;
        this.capacity = capacity;
    }
}
