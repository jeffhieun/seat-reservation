package com.linkz.reservation.database;

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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class OptimisticLockingIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void seatShouldThrowOptimisticLockExceptionOnStaleUpdate() {
        Seat created = seatRepository.saveAndFlush(Seat.builder()
                .seatNumber("OPT-SEAT-1")
                .status(SeatStatus.AVAILABLE)
                .build());

        Seat firstSnapshot = seatRepository.findById(created.getId()).orElseThrow();
        Seat secondSnapshot = seatRepository.findById(created.getId()).orElseThrow();

        firstSnapshot.setStatus(SeatStatus.PENDING_PAYMENT);
        Seat updated = seatRepository.saveAndFlush(firstSnapshot);
        assertThat(updated.getVersion()).isGreaterThan(0L);

        secondSnapshot.setStatus(SeatStatus.RESERVED);
        assertThatThrownBy(() -> seatRepository.saveAndFlush(secondSnapshot))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void reservationShouldThrowOptimisticLockExceptionOnStaleUpdate() {
        User user = userRepository.saveAndFlush(User.builder()
                .email("optimistic-reservation@test.com")
                .passwordHash("hash")
                .build());
        Seat seat = seatRepository.saveAndFlush(Seat.builder()
                .seatNumber("OPT-RES-1")
                .status(SeatStatus.PENDING_PAYMENT)
                .build());
        Reservation created = reservationRepository.saveAndFlush(Reservation.builder()
                .user(user)
                .seat(seat)
                .status(ReservationStatus.PENDING_PAYMENT)
                .build());

        Reservation firstSnapshot = reservationRepository.findById(created.getId()).orElseThrow();
        Reservation secondSnapshot = reservationRepository.findById(created.getId()).orElseThrow();

        firstSnapshot.setStatus(ReservationStatus.CONFIRMED);
        firstSnapshot.setConfirmedAt(LocalDateTime.now());
        Reservation updated = reservationRepository.saveAndFlush(firstSnapshot);
        assertThat(updated.getVersion()).isGreaterThan(0L);

        secondSnapshot.setStatus(ReservationStatus.EXPIRED);
        secondSnapshot.setExpiredAt(LocalDateTime.now());
        assertThatThrownBy(() -> reservationRepository.saveAndFlush(secondSnapshot))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void paymentShouldThrowOptimisticLockExceptionOnStaleUpdate() {
        User user = userRepository.saveAndFlush(User.builder()
                .email("optimistic-payment@test.com")
                .passwordHash("hash")
                .build());
        Seat seat = seatRepository.saveAndFlush(Seat.builder()
                .seatNumber("OPT-PAY-1")
                .status(SeatStatus.PENDING_PAYMENT)
                .build());
        Reservation reservation = reservationRepository.saveAndFlush(Reservation.builder()
                .user(user)
                .seat(seat)
                .status(ReservationStatus.PENDING_PAYMENT)
                .build());
        Payment created = paymentRepository.saveAndFlush(Payment.builder()
                .reservation(reservation)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("10.00"))
                .providerReference("OPT-REF-1")
                .build());

        Payment firstSnapshot = paymentRepository.findById(created.getId()).orElseThrow();
        Payment secondSnapshot = paymentRepository.findById(created.getId()).orElseThrow();

        firstSnapshot.setStatus(PaymentStatus.SUCCESS);
        Payment updated = paymentRepository.saveAndFlush(firstSnapshot);
        assertThat(updated.getVersion()).isGreaterThan(0L);

        secondSnapshot.setStatus(PaymentStatus.FAILED);
        assertThatThrownBy(() -> paymentRepository.saveAndFlush(secondSnapshot))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
