package com.linkz.reservation.scheduler;

import com.linkz.reservation.reservation.Reservation;
import com.linkz.reservation.reservation.ReservationProperties;
import com.linkz.reservation.reservation.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationScheduler {
    
    private final ReservationService reservationService;
    private final ReservationProperties reservationProperties;
    
    @Scheduled(fixedDelayString = "${reservation.scheduler-interval-seconds:60}000")
    public void releaseExpiredReservations() {
        try {
            List<Reservation> expiredReservations = reservationService.getExpiredReservations();
            
            if (!expiredReservations.isEmpty()) {
                log.info("Found {} expired reservations to release", expiredReservations.size());
                
                expiredReservations.forEach(reservation -> {
                    try {
                        reservationService.expireReservation(reservation);
                        log.debug("Released seat for expired reservation: {}", reservation.getId());
                    } catch (Exception e) {
                        log.error("Error releasing reservation {}: {}", reservation.getId(), e.getMessage(), e);
                    }
                });
                
                log.info("Successfully processed {} expired reservations", expiredReservations.size());
            }
        } catch (Exception e) {
            log.error("Error in expiration scheduler", e);
        }
    }
}

