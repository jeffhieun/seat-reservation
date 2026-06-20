package com.linkz.reservation.webhook;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_events", indexes = {
    @Index(name = "idx_webhook_events_event_id", columnList = "event_id", unique = true)
})
@SequenceGenerator(name = "webhook_events_seq_gen", sequenceName = "webhook_events_seq", allocationSize = 50)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "webhook_events_seq_gen")
    private Long id;
    
    @Column(nullable = false, unique = true, length = 255)
    private String eventId;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime processedAt;
}

