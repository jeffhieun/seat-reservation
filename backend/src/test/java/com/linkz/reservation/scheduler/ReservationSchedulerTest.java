package com.linkz.reservation.scheduler;

import com.linkz.reservation.reservation.InvalidReservationTransitionException;
import com.linkz.reservation.reservation.ReservationNotFoundException;
import com.linkz.reservation.reservation.ReservationService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationSchedulerTest {

    @Mock
    private ReservationService reservationService;

    private SimpleMeterRegistry meterRegistry;
    private Logger schedulerLogger;
    private ListAppender<ILoggingEvent> appender;

    @InjectMocks
    private ReservationScheduler reservationScheduler;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        schedulerLogger = (Logger) LoggerFactory.getLogger(ReservationScheduler.class);
        appender = new ListAppender<>();
        appender.start();
        schedulerLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        schedulerLogger.detachAppender(appender);
        meterRegistry.close();
    }

    @Test
    void shouldProcessExpiredReservationsAndRecordMetrics() {
        reservationScheduler = new ReservationScheduler(reservationService, meterRegistry);
        when(reservationService.getExpiredReservationIds()).thenReturn(List.of(1L, 2L));
        when(reservationService.expireReservation(1L)).thenReturn(reservation(1L));
        when(reservationService.expireReservation(2L)).thenReturn(reservation(2L));

        reservationScheduler.releaseExpiredReservations();

        verify(reservationService).expireReservation(1L);
        verify(reservationService).expireReservation(2L);
        assertThat(meterRegistry.counter("reservation.expiration.executions").count()).isEqualTo(1.0d);
        assertThat(meterRegistry.counter("reservation.expiration.success").count()).isEqualTo(2.0d);
        assertThat(meterRegistry.counter("reservation.expiration.failed").count()).isEqualTo(0.0d);
        assertThat(meterRegistry.timer("reservation.expiration.duration").count()).isEqualTo(1L);
        assertThat(summaryLogs()).anyMatch(message -> message.contains("Reservation expiration completed processed=2 expired=2 failed=0"));
    }

    @Test
    void shouldContinueProcessingAfterSingleReservationFailure() {
        reservationScheduler = new ReservationScheduler(reservationService, meterRegistry);
        when(reservationService.getExpiredReservationIds()).thenReturn(List.of(1L, 2L, 3L));
        when(reservationService.expireReservation(1L)).thenReturn(reservation(1L));
        doThrow(new InvalidReservationTransitionException("invalid transition")).when(reservationService).expireReservation(2L);
        when(reservationService.expireReservation(3L)).thenReturn(reservation(3L));

        reservationScheduler.releaseExpiredReservations();

        verify(reservationService).expireReservation(1L);
        verify(reservationService).expireReservation(2L);
        verify(reservationService).expireReservation(3L);
        assertThat(meterRegistry.counter("reservation.expiration.executions").count()).isEqualTo(1.0d);
        assertThat(meterRegistry.counter("reservation.expiration.success").count()).isEqualTo(2.0d);
        assertThat(meterRegistry.counter("reservation.expiration.failed").count()).isEqualTo(1.0d);
        assertThat(summaryLogs()).anyMatch(message -> message.contains("Reservation expiration completed processed=3 expired=2 failed=1"));
    }

    @Test
    void shouldLogWarnWhenFailureRateIsHigh() {
        reservationScheduler = new ReservationScheduler(reservationService, meterRegistry);
        when(reservationService.getExpiredReservationIds()).thenReturn(List.of(1L, 2L, 3L));
        doThrow(new ReservationNotFoundException("missing")).when(reservationService).expireReservation(1L);
        doThrow(new InvalidReservationTransitionException("invalid")).when(reservationService).expireReservation(2L);
        doThrow(new RuntimeException("boom")).when(reservationService).expireReservation(3L);

        reservationScheduler.releaseExpiredReservations();

        assertThat(summaryLogs(Level.WARN)).anyMatch(message ->
                message.contains("High reservation expiration failure rate detected"));
        assertThat(meterRegistry.counter("reservation.expiration.failed").count()).isEqualTo(3.0d);
    }

    private com.linkz.reservation.reservation.Reservation reservation(Long id) {
        com.linkz.reservation.seat.Seat seat = com.linkz.reservation.seat.Seat.builder()
                .id(id * 10)
                .seatNumber("S" + id)
                .status(com.linkz.reservation.seat.SeatStatus.AVAILABLE)
                .build();
        com.linkz.reservation.user.User user = com.linkz.reservation.user.User.builder()
                .id(id * 100)
                .email("user" + id + "@test.com")
                .passwordHash("hashed")
                .build();

        return com.linkz.reservation.reservation.Reservation.builder()
                .id(id)
                .seat(seat)
                .user(user)
                .status(com.linkz.reservation.reservation.ReservationStatus.EXPIRED)
                .build();
    }

    private List<String> summaryLogs() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    private List<String> summaryLogs(Level level) {
        return appender.list.stream()
                .filter(event -> event.getLevel().equals(level))
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }
}

