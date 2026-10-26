package com.staylab.room.dto;

import java.time.LocalDate;

public record DailyAvailability(LocalDate date, long totalRooms, long bookedRooms, long remainingRooms) {
}
