package com.linkz.reservation.reservation;

import com.linkz.reservation.seat.Seat;
import com.linkz.reservation.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations", indexes = {
    @Index(name = "idx_reservations_user_id", columnList = "user_id"),
    @Index(name = "idx_reservations_seat_id", columnList = "seat_id"),
    @Index(name = "idx_reservations_status", columnList = "status"),
    @Index(name = "idx_reservations_created_at", columnList = "created_at"),
    @Index(name = "idx_reservations_status_created_at", columnList = "status,created_at")
})
@SequenceGenerator(name = "reservations_seq_gen", sequenceName = "reservations_seq", allocationSize = 50)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reservations_seq_gen")
    private Long id;
    
    /**
     * Reservation references an existing user; no cascade to avoid accidental user lifecycle changes.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * Reservation references an existing seat; no cascade to avoid accidental seat delete/update propagation.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;
    
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
    
    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    /**
     * Optimistic locking detects concurrent modifications to reservation status transitions.
     */
    @Version
    @Column(nullable = false)
    private Long version;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
