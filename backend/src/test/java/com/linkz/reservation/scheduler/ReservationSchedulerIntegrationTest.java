package com.linkz.reservation.scheduler;

import com.linkz.reservation.reservation.Reservation;
import com.linkz.reservation.reservation.ReservationRepository;
import com.linkz.reservation.reservation.ReservationService;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "reservation.expiration-minutes=1",
        "reservation.scheduler-interval-seconds=5"
})
class ReservationSchedulerIntegrationTest {

    @Autowired
    private ReservationScheduler reservationScheduler;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;
    private Long seatId;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
        userRepository.deleteAll();

        User user = userRepository.save(User.builder()
                .email("scheduler-int@test.com")
                .passwordHash("hash")
                .build());
        userId = user.getId();

        Seat seat = seatRepository.save(Seat.builder()
                .seatNumber("SCH-1")
                .status(SeatStatus.AVAILABLE)
                .build());
        seatId = seat.getId();
    }

    @Test
    void schedulerShouldExpireOldPendingReservationAndReleaseSeat() {
        Reservation reservation = reservationService.reserveSeat(userId, seatId);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
        assertThat(seatRepository.findById(seatId)).get()
                .extracting(Seat::getStatus)
                .isEqualTo(SeatStatus.PENDING_PAYMENT);

        LocalDateTime oldCreatedAt = LocalDateTime.now().minusMinutes(5);
        jdbcTemplate.update(
                "UPDATE reservations SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(oldCreatedAt),
                reservation.getId()
        );

        reservationScheduler.releaseExpiredReservations();

        Reservation updated = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(updated.getExpiredAt()).isNotNull();
        assertThat(seatRepository.findById(seatId)).get()
                .extracting(Seat::getStatus)
                .isEqualTo(SeatStatus.AVAILABLE);
    }
}
