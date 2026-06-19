package com.linkz.reservation.seat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatService {
    
    private final SeatRepository seatRepository;
    
    public List<Seat> getAvailableSeats() {
        return seatRepository.findByStatus(SeatStatus.AVAILABLE);
    }
    
    public Seat getSeatById(Long seatId) {
        return seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Seat not found"));
    }
    
    public List<Seat> getAllSeats() {
        return seatRepository.findAll();
    }
}

