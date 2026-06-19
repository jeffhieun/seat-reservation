package com.linkz.reservation.scheduler;

import com.linkz.reservation.reservation.Reservation;
import com.linkz.reservation.reservation.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationScheduler {
    
    private final ReservationService reservationService;
    
    /**
     * Release seats that are pending payment for more than 10 minutes.
     * Runs every minute.
     */
    @Scheduled(fixedRate = 60000)  // Every 60 seconds
    public void releaseExpiredReservations() {
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        
        List<Reservation> expiredReservations = reservationService.getExpiredReservations(tenMinutesAgo);
        
        if (!expiredReservations.isEmpty()) {
            log.info("Found {} expired reservations", expiredReservations.size());
            
            expiredReservations.forEach(reservation -> {
                try {
                    reservationService.expireReservation(reservation);
                    log.info("Released seat for expired reservation: {}", reservation.getId());
                } catch (Exception e) {
                    log.error("Error releasing reservation {}: {}", reservation.getId(), e.getMessage());
                }
            });
        }
    }
}

