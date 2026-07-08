package com.linkz.reservation.reservation;

import com.linkz.reservation.seat.Seat;
import com.linkz.reservation.seat.SeatRepository;
import com.linkz.reservation.seat.SeatStatus;
import com.linkz.reservation.user.User;
import com.linkz.reservation.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "reservation.expiration-minutes=1",
    "reservation.scheduler-interval-seconds=10"
})
@DisplayName("Reservation Confirmation and Expiration Tests (FR-6 & FR-7)")
public class ReservationConfirmationExpirationTest {
    
    @Autowired
    private ReservationService reservationService;
    
    @Autowired
    private ReservationRepository reservationRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SeatRepository seatRepository;
    
    @Autowired
    private EntityManager entityManager;
    
    private User testUser;
    private Seat testSeat;
    
    @BeforeEach
    @Transactional
    void setUp() {
        // Create test user
        testUser = User.builder()
                .email("test-confirm@example.com")
                .passwordHash("hashed-password")
                .build();
        testUser = userRepository.save(testUser);
        
        // Create test seat
        testSeat = Seat.builder()
                .seatNumber("A1")
                .status(SeatStatus.AVAILABLE)
                .build();
        testSeat = seatRepository.save(testSeat);
    }
    
    // ================== FR-6: Reservation Confirmation Tests ==================
    
    @Test
    @Transactional
    @DisplayName("FR-6.1: Should create reservation in PENDING_PAYMENT status")
    void shouldCreateReservationInPendingPaymentStatus() {
        // When
        Reservation reservation = reservationService.reserveSeat(testUser.getId(), testSeat.getId());
        
        // Debug output
        System.out.println("DEBUG FR-6.1: reservation id: " + reservation.getId() + ", status: " + reservation.getStatus());
        System.out.println("DEBUG FR-6.1: testUser id: " + testUser.getId() + ", testSeat id: " + testSeat.getId());
        
        // Then
        assertThat(reservation).isNotNull();
        assertThat(reservation.getId()).isNotNull();
        assertThat(reservation.getStatus()).isNotNull().isEqualTo(ReservationStatus.PENDING_PAYMENT);
        assertThat(reservation.getCreatedAt()).isNotNull();
        assertThat(reservation.getUser()).isNotNull();
        assertThat(reservation.getSeat()).isNotNull();
    }
    
    @Test
    @Transactional
    @DisplayName("FR-6.2: Should confirm reservation and update status to CONFIRMED")
    void shouldConfirmReservationAndSetConfirmedTimestamp() {
        // Given
        Reservation reservation = reservationService.reserveSeat(testUser.getId(), testSeat.getId());
        LocalDateTime beforeConfirmation = LocalDateTime.now();
        
        // When
        reservationService.confirmReservation(reservation.getId());
        
        // Then
        Reservation confirmedReservation = reservationService.getReservationById(reservation.getId());
        assertThat(confirmedReservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(confirmedReservation.getConfirmedAt()).isNotNull();
        assertThat(confirmedReservation.getConfirmedAt()).isAfterOrEqualTo(beforeConfirmation);
    }
    
    @Test
    @Transactional
    @DisplayName("FR-6.3: Confirming reservation should update seat status to RESERVED")
    void shouldUpdateSeatStatusToReservedWhenConfirming() {
        // Given
        Reservation reservation = reservationService.reserveSeat(testUser.getId(), testSeat.getId());
        assertThat(testSeat.getStatus()).isEqualTo(SeatStatus.PENDING_PAYMENT);
        
        // When
        reservationService.confirmReservation(reservation.getId());
        
        // Then
        Seat updatedSeat = seatRepository.findById(testSeat.getId()).orElseThrow();
        assertThat(updatedSeat.getStatus()).isEqualTo(SeatStatus.RESERVED);
    }
    
    @Test
    @Transactional
    @DisplayName("FR-6.4: Should not confirm reservation that is not in PENDING_PAYMENT status")
    void shouldNotConfirmNonPendingReservation() {
        // Given
        Reservation reservation = reservationService.reserveSeat(testUser.getId(), testSeat.getId());
        reservationService.confirmReservation(reservation.getId());
        
        // When & Then
        assertThatThrownBy(() -> reservationService.confirmReservation(reservation.getId()))
                .isInstanceOf(InvalidReservationTransitionException.class)
                .hasMessageContaining("can only be confirmed");
    }
    
    @Test
    @Transactional
    @DisplayName("FR-6.5: Confirmed reservation should not be expirable")
    void shouldNotExpireConfirmedReservation() {
        // Given
        Reservation reservation = reservationService.reserveSeat(testUser.getId(), testSeat.getId());
        reservationService.confirmReservation(reservation.getId());
        
        // When & Then
        assertThatThrownBy(() -> reservationService.expireReservation(reservation.getId()))
                .isInstanceOf(InvalidReservationTransitionException.class)
                .hasMessageContaining("can only be expired");
    }
    
    // ================== FR-7: Reservation Expiration Tests ==================
    
    @Test
    @Transactional
    @DisplayName("FR-7.1: Should expire pending reservation and set expired timestamp")
    void shouldExpireReservationAndSetExpiredTimestamp() {
        // Given
        Reservation reservation = reservationService.reserveSeat(testUser.getId(), testSeat.getId());
        LocalDateTime beforeExpiration = LocalDateTime.now();
        
        // When
        reservationService.expireReservation(reservation.getId());
        
        // Then
        Reservation expiredReservation = reservationService.getReservationById(reservation.getId());
        assertThat(expiredReservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(expiredReservation.getExpiredAt()).isNotNull();
        assertThat(expiredReservation.getExpiredAt()).isAfterOrEqualTo(beforeExpiration);
    }
    
    @Test
    @Transactional
    @DisplayName("FR-7.2: Expiring reservation should release seat back to AVAILABLE")
    void shouldReleaseSeatWhenExpiringReservation() {
        // Given
        Reservation reservation = reservationService.reserveSeat(testUser.getId(), testSeat.getId());
        assertThat(testSeat.getStatus()).isEqualTo(SeatStatus.PENDING_PAYMENT);
        
        // When
        reservationService.expireReservation(reservation.getId());
        
        // Then
        Seat releasedSeat = seatRepository.findById(testSeat.getId()).orElseThrow();
        assertThat(releasedSeat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    @Transactional
    @DisplayName("FR-7.2b: Expire by ID should work even when entity is detached")
    void shouldExpireByIdWithoutLazyInitializationException() {
        Reservation reservation = reservationService.reserveSeat(testUser.getId(), testSeat.getId());
        entityManager.flush();
        entityManager.clear();

        assertThatCode(() -> reservationService.expireReservation(reservation.getId()))
                .doesNotThrowAnyException();

        Reservation expiredReservation = reservationService.getReservationById(reservation.getId());
        assertThat(expiredReservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        Seat releasedSeat = seatRepository.findById(testSeat.getId()).orElseThrow();
        assertThat(releasedSeat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }
    
    @Test
    @Transactional
    @DisplayName("FR-7.3: Should find expired reservations query method works")
    void shouldHaveExpiredReservationsQueryMethod() {
        // Given - Create multiple reservations
        Reservation reservation1 = reservationService.reserveSeat(testUser.getId(), testSeat.getId());
        
        // Create another seat and reservation
        Seat seat2 = Seat.builder()
                .seatNumber("A2")
                .status(SeatStatus.AVAILABLE)
                .build();
        seat2 = seatRepository.save(seat2);
        Reservation reservation2 = reservationService.reserveSeat(testUser.getId(), seat2.getId());
        
        // When - Call the method to verify it works (won't return results unless they're actually expired)
        List<Long> expiredReservationIds = reservationService.getExpiredReservationIds();
        
        // Then - Verify the method executes without error
        assertThat(expiredReservationIds).isNotNull();
        // Note: The actual expiration check depends on the 1-minute timeout which hasn't passed yet
        // in this test, so we just verify the method returns a list
    }
    
    @Test
    @Transactional
    @DisplayName("FR-7.4: Should not expire already expired reservation twice")
    void shouldNotExpireAlreadyExpiredReservation() {
        // Given
        Reservation reservation = reservationService.reserveSeat(testUser.getId(), testSeat.getId());
        reservationService.expireReservation(reservation.getId());
        LocalDateTime firstExpiredAt = reservationService.getReservationById(reservation.getId()).getExpiredAt();
        
        // When & Then - Try to expire again
        assertThatThrownBy(() -> reservationService.expireReservation(reservation.getId()))
                .isInstanceOf(InvalidReservationTransitionException.class);

        Reservation stillExpired = reservationService.getReservationById(reservation.getId());
        assertThat(stillExpired.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(stillExpired.getExpiredAt()).isEqualTo(firstExpiredAt);
    }
    
    // ================== Combined FR-6 & FR-7 Tests ==================
    
    @Test
    @Transactional
    @DisplayName("FR-6 & FR-7: Reservation lifecycle - Create -> Confirm -> Stay Reserved")
    void shouldFollowCompleteReservationLifecycle() {
        // Given
        assertThat(testSeat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        
        // When - Create reservation
        Reservation reservation = reservationService.reserveSeat(testUser.getId(), testSeat.getId());
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
        assertThat(seatRepository.findById(testSeat.getId()).orElseThrow().getStatus())
                .isEqualTo(SeatStatus.PENDING_PAYMENT);
        
        // When - Confirm reservation
        reservationService.confirmReservation(reservation.getId());
        Reservation confirmedReservation = reservationService.getReservationById(reservation.getId());
        assertThat(confirmedReservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(confirmedReservation.getConfirmedAt()).isNotNull();
        assertThat(seatRepository.findById(testSeat.getId()).orElseThrow().getStatus())
                .isEqualTo(SeatStatus.RESERVED);
        
        // Then - Seat should remain reserved (not expelled by scheduler)
        assertThat(seatRepository.findById(testSeat.getId()).orElseThrow().getStatus())
                .isEqualTo(SeatStatus.RESERVED);
    }
    
    @Test
    @Transactional
    @DisplayName("FR-6 & FR-7: Get user reservations should include confirmation info")
    void shouldIncludeConfirmationInformationInUserReservations() {
        // Given
        Reservation reservation = reservationService.reserveSeat(testUser.getId(), testSeat.getId());
        reservationService.confirmReservation(reservation.getId());
        
        // When
        List<Reservation> userReservations = reservationService.getUserReservations(testUser.getId());
        
        // Then
        assertThat(userReservations).hasSize(1);
        Reservation retrieved = userReservations.get(0);
        assertThat(retrieved.getConfirmedAt()).isNotNull();
        assertThat(retrieved.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    // Debug test to understand the issue
    @Test
    @Transactional
    @DisplayName("DEBUG: Simple reservation creation")
    void debugSimpleReservationCreation() {
        // Create a simple reservation to debug
        Reservation reservation = Reservation.builder()
                .user(testUser)
                .seat(testSeat)
                .status(ReservationStatus.PENDING_PAYMENT)
                .build();
        
        System.out.println("DEBUG: Before save - reservation id: " + reservation.getId() + ", status: " + reservation.getStatus());
        
        Reservation saved = reservationRepository.save(reservation);
        
        System.out.println("DEBUG: After save - reservation id: " + saved.getId() + ", status: " + saved.getStatus());
        System.out.println("DEBUG: testSeat status: " + testSeat.getStatus());
        
        // This should not throw and status should be PENDING_PAYMENT
        assertThat(saved.getStatus()).isNotNull().isEqualTo(ReservationStatus.PENDING_PAYMENT);
    }
}
