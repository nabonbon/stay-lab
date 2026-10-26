package com.staylab.reservation;

import com.staylab.reservation.dto.ReservationRequest;
import com.staylab.reservation.dto.ReservationResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponse> reserve(@Valid @RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService.reserve(request);
        return ResponseEntity.created(URI.create("/api/reservations/" + response.reservationId())).body(response);
    }

    @GetMapping("/{reservationId}")
    public ReservationResponse findById(@PathVariable Long reservationId) {
        return reservationService.findById(reservationId);
    }

    @GetMapping
    public List<ReservationResponse> findByGuestPhone(@RequestParam String phone) {
        return reservationService.findByGuestPhone(phone);
    }

    @PostMapping("/{reservationId}/cancel")
    public ReservationResponse cancel(@PathVariable Long reservationId) {
        return reservationService.cancel(reservationId);
    }
}
