package com.linkz.reservation.scheduler;

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
    
    @Scheduled(fixedDelayString = "${reservation.scheduler-interval-seconds:60}000")
    public void releaseExpiredReservations() {
        try {
            List<Long> expiredReservationIds = reservationService.getExpiredReservationIds();
            
            if (!expiredReservationIds.isEmpty()) {
                log.info("Found {} expired reservations to release", expiredReservationIds.size());
                
                expiredReservationIds.forEach(reservationId -> {
                    try {
                        reservationService.expireReservation(reservationId);
                        log.debug("Released seat for expired reservation: {}", reservationId);
                    } catch (Exception e) {
                        log.error("Error releasing reservation {}: {}", reservationId, e.getMessage(), e);
                    }
                });
                
                log.info("Finished processing {} expired reservations", expiredReservationIds.size());
            }
        } catch (Exception e) {
            log.error("Error in expiration scheduler", e);
        }
    }
}
