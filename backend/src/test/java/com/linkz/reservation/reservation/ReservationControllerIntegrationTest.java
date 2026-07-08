package com.linkz.reservation.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkz.reservation.auth.LoginRequest;
import com.linkz.reservation.auth.LoginResponse;
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

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String ownerEmail;
    private String otherEmail;
    private Long seatId;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
        userRepository.deleteAll();

        ownerEmail = "reservation-owner-" + System.nanoTime() + "@test.com";
        otherEmail = "reservation-other-" + System.nanoTime() + "@test.com";

        userRepository.save(User.builder()
                .email(ownerEmail)
                .passwordHash(passwordEncoder.encode("password123"))
                .build());
        userRepository.save(User.builder()
                .email(otherEmail)
                .passwordHash(passwordEncoder.encode("password123"))
                .build());

        seatId = seatRepository.save(Seat.builder()
                .seatNumber("RC1")
                .status(SeatStatus.AVAILABLE)
                .build()).getId();
    }

    @Test
    void reserveSeatShouldReturnCreatedDto() throws Exception {
        String token = login(ownerEmail);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("seatId", seatId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.seat_id").value(seatId))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
    }

    @Test
    void duplicateReservationShouldReturn409WithStandardPayload() throws Exception {
        String token = login(ownerEmail);

        mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("seatId", seatId))))
            .andExpect(status().isCreated());

        seatRepository.findById(seatId).ifPresent(seat -> {
            seat.setStatus(SeatStatus.AVAILABLE);
            seatRepository.saveAndFlush(seat);
        });

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("seatId", seatId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_RESERVATION"))
                .andExpect(jsonPath("$.message").value("You already have an active reservation for this seat."));
    }

    @Test
    void reservationDetailsShouldReturn404ForMissingId() throws Exception {
        String token = login(ownerEmail);

        mockMvc.perform(get("/api/reservations/{id}", 999999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESERVATION_NOT_FOUND"));
    }

    @Test
    void confirmAndExpireShouldFollowStateRules() throws Exception {
        String token = login(ownerEmail);

        String reserveBody = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("seatId", seatId))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ReservationResponse created = objectMapper.readValue(reserveBody, ReservationResponse.class);

        mockMvc.perform(post("/api/reservations/{id}/confirm", created.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(post("/api/reservations/{id}/expire", created.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_RESERVATION_TRANSITION"));
    }

    @Test
    void reservationOwnershipShouldBeEnforced() throws Exception {
        String ownerToken = login(ownerEmail);
        String otherToken = login(otherEmail);

        String reserveBody = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("seatId", seatId))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ReservationResponse created = objectMapper.readValue(reserveBody, ReservationResponse.class);

        mockMvc.perform(get("/api/reservations/{id}", created.id())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    private String login(String email) throws Exception {
        String responseBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "password123"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(responseBody, LoginResponse.class).token();
    }
}
