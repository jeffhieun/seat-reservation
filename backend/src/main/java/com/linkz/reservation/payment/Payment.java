package com.linkz.reservation.payment;

import com.linkz.reservation.reservation.Reservation;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_payments_reservation_id", columnNames = "reservation_id")
    },
    indexes = {
        @Index(name = "idx_payments_reservation_id", columnList = "reservation_id"),
        @Index(name = "idx_payments_status", columnList = "status"),
        @Index(name = "idx_payments_provider_reference", columnList = "provider_reference")
    })
@SequenceGenerator(name = "payments_seq_gen", sequenceName = "payments_seq", allocationSize = 50)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payments_seq_gen")
    private Long id;
    
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    
    @Column(name = "provider_reference", length = 255)
    private String providerReference;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

