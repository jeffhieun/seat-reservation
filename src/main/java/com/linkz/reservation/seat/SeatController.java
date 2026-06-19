package com.linkz.reservation.seat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {
    
    private final SeatService seatService;
    
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SeatResponse>> getSeats() {
        log.debug("Fetching available seats");
        List<SeatResponse> seats = seatService.getAvailableSeats()
                .stream()
                .map(SeatResponse::from)
                .toList();
        return ResponseEntity.ok(seats);
    }
}

