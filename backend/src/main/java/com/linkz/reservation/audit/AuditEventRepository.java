package com.linkz.reservation.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByOrderByCreatedAtDesc();
    List<AuditEvent> findByReservationIdOrderByCreatedAtDesc(Long reservationId);
    List<AuditEvent> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);
}

