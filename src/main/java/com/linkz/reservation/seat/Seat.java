package com.linkz.reservation.seat;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "seats", indexes = {
    @Index(name = "idx_seats_seat_number", columnList = "seat_number", unique = true),
    @Index(name = "idx_seats_status", columnList = "status")
})
@SequenceGenerator(name = "seats_seq_gen", sequenceName = "seats_seq", allocationSize = 50)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seats_seq_gen")
    private Long id;
    
    @Column(nullable = false, unique = true, length = 10)
    private String seatNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

