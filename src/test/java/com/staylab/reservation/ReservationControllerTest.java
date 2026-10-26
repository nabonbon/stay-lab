package com.staylab.reservation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/reservations: 예약 생성 후 201과 배정 객실을 돌려준다")
    void reserve() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(10);

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomTypeId": 2, "guestName": "홍길동", "guestPhone": "010-1234-5678",
                                 "checkIn": "%s", "checkOut": "%s"}
                                """.formatted(checkIn, checkIn.plusDays(1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomNo").value("201"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("POST /api/reservations: 전화번호 형식이 틀리면 400")
    void reserve_invalidPhone() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(10);

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomTypeId": 2, "guestName": "홍길동", "guestPhone": "1234",
                                 "checkIn": "%s", "checkOut": "%s"}
                                """.formatted(checkIn, checkIn.plusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("GET /api/reservations/{id}: 없는 예약이면 404")
    void findById_notFound() throws Exception {
        mockMvc.perform(get("/api/reservations/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));
    }
}
