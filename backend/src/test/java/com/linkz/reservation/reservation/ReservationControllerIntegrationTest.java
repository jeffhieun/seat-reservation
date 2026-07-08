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
    private Long firstSeatId;
    private Long secondSeatId;

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

        firstSeatId = seatRepository.save(Seat.builder()
                .seatNumber("RC1")
                .status(SeatStatus.AVAILABLE)
                .build()).getId();

        secondSeatId = seatRepository.save(Seat.builder()
                .seatNumber("RC2")
                .status(SeatStatus.AVAILABLE)
                .build()).getId();
    }

    @Test
    void scenario1ShouldReturn409WhenUserReservesSameSeatTwiceWhilePending() throws Exception {
        String token = login(ownerEmail);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("seatId", firstSeatId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.seat_id").value(firstSeatId))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("seatId", firstSeatId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("You already have an active reservation for this seat."))
                .andExpect(jsonPath("$.path").value("/api/reservations"));
    }

    @Test
    void scenario2ShouldAllowReserveAgainAfterExpired() throws Exception {
        String token = login(ownerEmail);

        String reserveBody = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("seatId", firstSeatId))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ReservationResponse created = objectMapper.readValue(reserveBody, ReservationResponse.class);

        mockMvc.perform(post("/api/reservations/{id}/expire", created.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"));

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("seatId", firstSeatId))))
                .andExpect(status().isCreated());
    }

    @Test
    void scenario3ShouldReturn409WhenUserReservesSameSeatAfterConfirmed() throws Exception {
        String token = login(ownerEmail);

        String reserveBody = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("seatId", firstSeatId))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ReservationResponse created = objectMapper.readValue(reserveBody, ReservationResponse.class);

        mockMvc.perform(post("/api/reservations/{id}/confirm", created.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("seatId", firstSeatId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("You already have an active reservation for this seat."));
    }

    @Test
    void scenario4ShouldAllowReservingAnotherSeat() throws Exception {
        String token = login(ownerEmail);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("seatId", firstSeatId))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("seatId", secondSeatId))))
                .andExpect(status().isCreated());
    }

    @Test
    void scenario5DifferentUserShouldKeepSeatLockingBehavior() throws Exception {
        String ownerToken = login(ownerEmail);
        String otherToken = login(otherEmail);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("seatId", firstSeatId))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("seatId", firstSeatId))))
                .andExpect(status().isConflict());
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
