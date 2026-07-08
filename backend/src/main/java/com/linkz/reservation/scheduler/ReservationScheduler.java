package com.linkz.reservation.scheduler;

import com.linkz.reservation.reservation.InvalidReservationTransitionException;
import com.linkz.reservation.reservation.ReservationNotFoundException;
import com.linkz.reservation.reservation.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationScheduler {
    
    private final ReservationService reservationService;
    private final MeterRegistry meterRegistry;
    
    @Scheduled(fixedDelayString = "${reservation.scheduler-interval-seconds:60}000")
    public void releaseExpiredReservations() {
        Instant startedAt = Instant.now();
        Counter executions = meterRegistry.counter("reservation.expiration.executions");
        Counter successCounter = meterRegistry.counter("reservation.expiration.success");
        Counter failedCounter = meterRegistry.counter("reservation.expiration.failed");
        Timer timer = meterRegistry.timer("reservation.expiration.duration");

        int processed = 0;
        int expired = 0;
        int failed = 0;

        executions.increment();

        try {
            List<Long> expiredReservationIds = reservationService.getExpiredReservationIds();
            processed = expiredReservationIds.size();
            log.info("Reservation expiration scan found {} candidate reservations", processed);

            for (Long reservationId : expiredReservationIds) {
                Instant reservationStartedAt = Instant.now();
                try {
                    var expiredReservation = reservationService.expireReservation(reservationId);
                    expired++;
                    successCounter.increment();
                    long executionTimeMs = Duration.between(reservationStartedAt, Instant.now()).toMillis();

                    log.info(
                            "Reservation expired successfully reservationId={} seatId={} userId={} oldStatus={} newStatus={} executionTimeMs={}",
                            expiredReservation.getId(),
                            expiredReservation.getSeat().getId(),
                            expiredReservation.getUser().getId(),
                            "PENDING_PAYMENT",
                            expiredReservation.getStatus().name(),
                            executionTimeMs
                    );
                } catch (ReservationNotFoundException | InvalidReservationTransitionException | DataAccessException e) {
                    failed++;
                    failedCounter.increment();
                    long executionTimeMs = Duration.between(reservationStartedAt, Instant.now()).toMillis();
                    log.warn(
                            "Reservation expiration failed reservationId={} exceptionType={} message={} executionTimeMs={}",
                            reservationId,
                            e.getClass().getSimpleName(),
                            e.getMessage(),
                            executionTimeMs,
                            e
                    );
                } catch (RuntimeException e) {
                    failed++;
                    failedCounter.increment();
                    long executionTimeMs = Duration.between(reservationStartedAt, Instant.now()).toMillis();
                    log.error(
                            "Unexpected reservation expiration failure reservationId={} exceptionType={} message={} executionTimeMs={}",
                            reservationId,
                            e.getClass().getSimpleName(),
                            e.getMessage(),
                            executionTimeMs,
                            e
                    );
                }
            }
        } catch (DataAccessException e) {
            failedCounter.increment();
            log.error("Reservation expiration scheduler failed to load expired reservation ids", e);
            failed = 1;
        } catch (RuntimeException e) {
            failedCounter.increment();
            log.error("Unexpected reservation expiration scheduler failure", e);
            failed = 1;
        } finally {
            long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
            timer.record(Duration.ofMillis(durationMs));

            double failureRate = processed == 0 ? 0.0 : ((double) failed / processed) * 100.0;
            if (failureRate > 20.0) {
                log.warn(
                        "High reservation expiration failure rate detected processed={} expired={} failed={} durationMs={} failureRate={}",
                        processed,
                        expired,
                        failed,
                        durationMs,
                        String.format("%.2f", failureRate)
                );
            }

            log.info(
                "Reservation expiration completed processed={} expired={} failed={} durationMs={}",
                processed,
                expired,
                failed,
                durationMs
            );
        }
    }
}
