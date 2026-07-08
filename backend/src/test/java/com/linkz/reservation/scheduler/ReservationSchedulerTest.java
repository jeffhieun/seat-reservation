package com.linkz.reservation.scheduler;

import com.linkz.reservation.reservation.ReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationSchedulerTest {

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private ReservationScheduler reservationScheduler;

    @Test
    void shouldContinueProcessingExpiredReservationsWhenOneFails() {
        when(reservationService.getExpiredReservationIds()).thenReturn(List.of(1L, 2L, 3L));
        doThrow(new RuntimeException("boom")).when(reservationService).expireReservation(2L);

        reservationScheduler.releaseExpiredReservations();

        verify(reservationService).expireReservation(1L);
        verify(reservationService).expireReservation(2L);
        verify(reservationService).expireReservation(3L);
    }
}

