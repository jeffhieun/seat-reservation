package com.linkz.reservation.repository;

import com.linkz.reservation.payment.Payment;
import com.linkz.reservation.payment.PaymentRepository;
import com.linkz.reservation.payment.PaymentStatus;
import com.linkz.reservation.reservation.Reservation;
import com.linkz.reservation.reservation.ReservationRepository;
import com.linkz.reservation.reservation.ReservationStatus;
import com.linkz.reservation.seat.Seat;
import com.linkz.reservation.seat.SeatRepository;
import com.linkz.reservation.seat.SeatStatus;
import com.linkz.reservation.user.User;
import com.linkz.reservation.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private User user;
    private Seat seat;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email("repo@test.com")
                .passwordHash("hash")
                .build());

        seat = seatRepository.save(Seat.builder()
                .seatNumber("R-1")
                .status(SeatStatus.PENDING_PAYMENT)
                .build());

        reservation = reservationRepository.save(Reservation.builder()
                .user(user)
                .seat(seat)
                .status(ReservationStatus.PENDING_PAYMENT)
                .build());
    }

    @Test
    void shouldFindReservationByUserAndSeat() {
        Optional<Reservation> found = reservationRepository.findByUserIdAndSeatId(user.getId(), seat.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(reservation.getId());
    }

    @Test
    void shouldLockSeatByIdForUpdate() {
        Optional<Seat> lockedSeat = seatRepository.findByIdForUpdate(seat.getId());
        assertThat(lockedSeat).isPresent();
        assertThat(lockedSeat.get().getId()).isEqualTo(seat.getId());
    }

    @Test
    void shouldEnforceSinglePaymentPerReservation() {
        paymentRepository.saveAndFlush(Payment.builder()
                .reservation(reservation)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("10.00"))
                .providerReference("PAY-1")
                .build());

        assertThatThrownBy(() -> paymentRepository.saveAndFlush(Payment.builder()
                .reservation(reservation)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("10.00"))
                .providerReference("PAY-2")
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldFindActiveReservationByUserAndSeat() {
        Optional<Reservation> active = reservationRepository.findActiveReservationByUserAndSeat(user.getId(), seat.getId());
        assertThat(active).isPresent();
        assertThat(active.get().getStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
    }
}
