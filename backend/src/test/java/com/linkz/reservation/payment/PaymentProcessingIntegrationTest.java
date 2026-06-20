package com.linkz.reservation.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkz.reservation.auth.LoginRequest;
import com.linkz.reservation.auth.LoginResponse;
import com.linkz.reservation.reservation.ReservationRepository;
import com.linkz.reservation.reservation.ReservationRequest;
import com.linkz.reservation.reservation.ReservationResponse;
import com.linkz.reservation.reservation.ReservationStatus;
import com.linkz.reservation.seat.Seat;
import com.linkz.reservation.seat.SeatRepository;
import com.linkz.reservation.seat.SeatStatus;
import com.linkz.reservation.user.User;
import com.linkz.reservation.user.UserRepository;
import com.linkz.reservation.webhook.WebhookEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentProcessingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long seatId;

    @BeforeEach
    void setUp() {
        webhookEventRepository.deleteAll();
        paymentRepository.deleteAll();
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
        userRepository.deleteAll();

        seatId = seatRepository.save(Seat.builder()
                .seatNumber("P001")
                .status(SeatStatus.AVAILABLE)
                .build()).getId();

        userRepository.save(User.builder()
                .email("payer1@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .build());

        userRepository.save(User.builder()
                .email("payer2@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .build());
    }

    @Test
    void ownerCanInitiateAndFetchPaymentWhileOthersCannot() throws Exception {
        String ownerToken = login("payer1@test.com", "password123");
        String otherToken = login("payer2@test.com", "password123");

        ReservationResponse reservation = reserveSeat(ownerToken);
        assertThat(seatRepository.findById(seatId)).get()
                .extracting(Seat::getStatus)
                .isEqualTo(SeatStatus.PENDING_PAYMENT);

        PaymentResponse payment = initiatePayment(ownerToken, reservation.id());
        assertThat(payment.reservationId()).isEqualTo(reservation.id());
        assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING.name());
        assertThat(payment.amount()).isEqualTo("10.00");
        assertThat(payment.providerReference()).startsWith("PAY_");

        PaymentResponse fetchedPayment = getPayment(ownerToken, payment.id());
        assertThat(fetchedPayment).isEqualTo(payment);

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("reservationId", reservation.id().toString()))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + otherToken)
                        .param("reservationId", reservation.id().toString()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/payments/{paymentId}", payment.id())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/webhooks/payment-success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "eventId", "evt-payment-success-1",
                                "providerReference", payment.providerReference()
                        ))))
                .andExpect(status().isOk());

        Payment paymentAfterWebhook = paymentRepository.findById(payment.id()).orElseThrow();
        assertThat(paymentAfterWebhook.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(reservationRepository.findById(reservation.id())).get()
                .extracting(res -> res.getStatus().name())
                .isEqualTo(ReservationStatus.CONFIRMED.name());
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

    private PaymentResponse getPayment(String token, Long paymentId) throws Exception {
        String responseBody = mockMvc.perform(get("/api/payments/{paymentId}", paymentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(responseBody, PaymentResponse.class);
    }
}

