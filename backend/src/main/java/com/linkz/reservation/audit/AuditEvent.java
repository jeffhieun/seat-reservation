package com.linkz.reservation.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_events", indexes = {
        @Index(name = "idx_audit_events_event_type", columnList = "event_type"),
        @Index(name = "idx_audit_events_created_at", columnList = "created_at"),
        @Index(name = "idx_audit_events_reservation_id", columnList = "reservation_id"),
        @Index(name = "idx_audit_events_payment_id", columnList = "payment_id")
})
@SequenceGenerator(name = "audit_events_seq_gen", sequenceName = "audit_events_seq", allocationSize = 50)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_events_seq_gen")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 100)
    private AuditEventType eventType;

    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "seat_id")
    private Long seatId;

    @Column(name = "reference_id", length = 255)
    private String referenceId;

    @Column(name = "details", length = 1000)
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

