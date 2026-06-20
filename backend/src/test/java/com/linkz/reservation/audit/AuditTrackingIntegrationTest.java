package com.linkz.reservation.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkz.reservation.auth.LoginRequest;
import com.linkz.reservation.auth.LoginResponse;
import com.linkz.reservation.payment.PaymentRepository;
import com.linkz.reservation.payment.PaymentResponse;
import com.linkz.reservation.payment.PaymentStatus;
import com.linkz.reservation.reservation.ReservationRequest;
import com.linkz.reservation.reservation.ReservationRepository;
import com.linkz.reservation.reservation.ReservationResponse;
import com.linkz.reservation.reservation.ReservationStatus;
import com.linkz.reservation.seat.Seat;
import com.linkz.reservation.seat.SeatRepository;
import com.linkz.reservation.seat.SeatStatus;
import com.linkz.reservation.user.User;
import com.linkz.reservation.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditTrackingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long seatId;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        paymentRepository.deleteAll();
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
        userRepository.deleteAll();

        seatId = seatRepository.save(Seat.builder()
                .seatNumber("Z001")
                .status(SeatStatus.AVAILABLE)
                .build()).getId();

        userRepository.save(User.builder()
                .email("audit@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .build());
    }

    @Test
    void recordsAuditEventsAcrossReservationPaymentAndWebhookFlow() throws Exception {
        String token = login("audit@test.com", "password123");
        ReservationResponse reservation = reserveSeat(token);
        PaymentResponse payment = initiatePayment(token, reservation.id());

        mockMvc.perform(post("/api/webhooks/payment-success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "eventId", "evt-audit-success-1",
                                "providerReference", payment.providerReference()
                        ))))
                .andExpect(status().isOk());

        List<AuditEvent> events = auditEventRepository.findByOrderByCreatedAtDesc();
        Set<AuditEventType> eventTypes = events.stream()
                .map(AuditEvent::getEventType)
                .collect(Collectors.toSet());
        assertThat(eventTypes).containsExactlyInAnyOrder(
                AuditEventType.RESERVATION_CREATED,
                AuditEventType.PAYMENT_INITIATED,
                AuditEventType.WEBHOOK_PAYMENT_SUCCESS_RECEIVED,
                AuditEventType.PAYMENT_SUCCESS,
                AuditEventType.RESERVATION_CONFIRMED
        );

        assertThat(reservationRepository.findById(reservation.id())).get()
                .extracting(res -> res.getStatus().name())
                .isEqualTo(ReservationStatus.CONFIRMED.name());
        assertThat(paymentRepository.findById(payment.id())).get()
                .extracting(p -> p.getStatus().name())
                .isEqualTo(PaymentStatus.SUCCESS.name());
        assertThat(seatRepository.findById(seatId)).get()
                .extracting(Seat::getStatus)
                .isEqualTo(SeatStatus.RESERVED);
    }

    private String login(String email, String password) throws Exception {
        String responseBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(responseBody, LoginResponse.class).token();
    }

    private ReservationResponse reserveSeat(String token) throws Exception {
        String responseBody = mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(new ReservationRequest(seatId))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(responseBody, ReservationResponse.class);
    }

    private PaymentResponse initiatePayment(String token, Long reservationId) throws Exception {
        String responseBody = mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + token)
                        .param("reservationId", reservationId.toString()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(responseBody, PaymentResponse.class);
    }
}

