package com.linkz.reservation.reservation;

import com.linkz.reservation.audit.AuditService;
import com.linkz.reservation.seat.Seat;
import com.linkz.reservation.seat.SeatRepository;
import com.linkz.reservation.seat.SeatStatus;
import com.linkz.reservation.user.User;
import com.linkz.reservation.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReservationProperties reservationProperties;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ReservationService reservationService;

    private User user;
    private Seat seat;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("user@test.com").passwordHash("hashed").build();
        seat = Seat.builder().id(10L).seatNumber("A01").status(SeatStatus.AVAILABLE).build();
    }

    @Test
    void reserveSeatShouldRejectDuplicateActiveReservation() {
        Reservation existing = Reservation.builder()
                .id(100L)
                .status(ReservationStatus.PENDING_PAYMENT)
                .user(user)
                .seat(seat)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reservationRepository.findActiveReservationByUserAndSeat(1L, 10L))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> reservationService.reserveSeat(1L, 10L))
                .isInstanceOf(DuplicateReservationException.class)
                .hasMessage("You already have an active reservation for this seat.");
    }

    @Test
    void confirmReservationShouldRejectInvalidTransition() {
        Reservation reservation = Reservation.builder()
                .id(100L)
                .status(ReservationStatus.EXPIRED)
                .user(user)
                .seat(Seat.builder().id(10L).seatNumber("A01").status(SeatStatus.AVAILABLE).build())
                .build();

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.confirmReservation(100L))
                .isInstanceOf(InvalidReservationTransitionException.class)
                .hasMessage("Reservation can only be confirmed from PENDING_PAYMENT.");
    }

    @Test
    void expireReservationShouldRejectNonOwner() {
        Reservation reservation = Reservation.builder()
                .id(100L)
                .status(ReservationStatus.PENDING_PAYMENT)
                .user(user)
                .seat(Seat.builder().id(10L).seatNumber("A01").status(SeatStatus.PENDING_PAYMENT).build())
                .build();

        when(reservationRepository.findByIdWithSeat(100L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.expireReservation(100L, 2L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You cannot access this reservation.");
    }

    @Test
    void reserveSeatShouldCreatePendingReservationAndLockSeat() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reservationRepository.findActiveReservationByUserAndSeat(1L, 10L))
                .thenReturn(Optional.empty());
        when(seatRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(seat));
        when(reservationRepository.saveAndFlush(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation input = invocation.getArgument(0);
            input.setId(200L);
            input.setCreatedAt(LocalDateTime.now());
            return input;
        });

        Reservation created = reservationService.reserveSeat(1L, 10L);

        assertThat(created.getId()).isEqualTo(200L);
        assertThat(created.getStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
        verify(seatRepository).saveAndFlush(seat);
        verify(auditService).recordReservationCreated(created);
    }

    @Test
    void getExpiredReservationIdsShouldUseConfiguredExpirationWindow() {
        when(reservationProperties.getExpirationMinutes()).thenReturn(10);
        when(reservationRepository.findIdsByStatusAndCreatedAtBefore(eq(ReservationStatus.PENDING_PAYMENT), any(LocalDateTime.class)))
                .thenReturn(List.of());

        List<Long> result = reservationService.getExpiredReservationIds();

        assertThat(result).isEmpty();
        verify(reservationRepository).findIdsByStatusAndCreatedAtBefore(eq(ReservationStatus.PENDING_PAYMENT), any(LocalDateTime.class));
        verify(auditService, never()).recordReservationExpired(any());
    }

    @Test
    void expireReservationShouldReloadWithSeatAndReleaseSeat() {
        Seat pendingSeat = Seat.builder().id(10L).seatNumber("A01").status(SeatStatus.PENDING_PAYMENT).build();
        Reservation pendingReservation = Reservation.builder()
                .id(100L)
                .status(ReservationStatus.PENDING_PAYMENT)
                .seat(pendingSeat)
                .user(user)
                .build();

        when(reservationRepository.findByIdWithSeat(100L)).thenReturn(Optional.of(pendingReservation));
        when(reservationRepository.saveAndFlush(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reservation expired = reservationService.expireReservation(100L);

        assertThat(expired.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(expired.getSeat().getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        verify(reservationRepository).findByIdWithSeat(100L);
        verify(auditService).recordReservationExpired(expired);
    }
}
