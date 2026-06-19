package com.linkz.reservation.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkz.reservation.auth.LoginRequest;
import com.linkz.reservation.auth.LoginResponse;
import com.linkz.reservation.payment.PaymentRepository;
import com.linkz.reservation.seat.Seat;
import com.linkz.reservation.seat.SeatResponse;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationConcurrencyIntegrationTest {

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
    private PaymentRepository paymentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long seatId;
    private String seatNumber;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
        userRepository.deleteAll();

        seatNumber = "C001";
        seatId = seatRepository.save(Seat.builder()
                .seatNumber(seatNumber)
                .status(SeatStatus.AVAILABLE)
                .build()).getId();

        userRepository.save(User.builder()
                .email("concurrent1@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .build());

        userRepository.save(User.builder()
                .email("concurrent2@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .build());
    }

    @Test
    void onlyOneReservationSucceedsForSameSeat() throws Exception {
        String token1 = login("concurrent1@test.com", "password123");
        String token2 = login("concurrent2@test.com", "password123");

        try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
            CountDownLatch readyLatch = new CountDownLatch(2);
            CountDownLatch startLatch = new CountDownLatch(1);

            Future<Integer> response1 = executorService.submit(() -> reserveSeat(token1, readyLatch, startLatch));
            Future<Integer> response2 = executorService.submit(() -> reserveSeat(token2, readyLatch, startLatch));

            assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
            startLatch.countDown();

            int status1 = response1.get();
            int status2 = response2.get();

            assertThat(status1).isIn(201, 409);
            assertThat(status2).isIn(201, 409);
            assertThat(status1).isNotEqualTo(status2);
        }

        assertThat(reservationRepository.count()).isEqualTo(1);
        assertThat(seatRepository.findById(seatId)).get()
                .extracting(Seat::getStatus)
                .isEqualTo(SeatStatus.PENDING_PAYMENT);
    }

    @Test
    void selectedSeatIsHiddenFromAvailableSeatsAndRejectedForOtherUsers() throws Exception {
        String token1 = login("concurrent1@test.com", "password123");
        String token2 = login("concurrent2@test.com", "password123");

        MvcResult createdReservation = mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token1)
                        .content(objectMapper.writeValueAsString(new ReservationRequest(seatId))))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(createdReservation.getResponse().getContentAsString()).isNotBlank();
        assertThat(seatRepository.findById(seatId)).get()
                .extracting(Seat::getStatus)
                .isEqualTo(SeatStatus.PENDING_PAYMENT);

        MvcResult seatsResult = mockMvc.perform(get("/api/seats")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andReturn();

        List<SeatResponse> availableSeats = Arrays.asList(objectMapper.readValue(
                seatsResult.getResponse().getContentAsByteArray(),
                SeatResponse[].class
        ));

        assertThat(availableSeats)
                .extracting(SeatResponse::seatNumber)
                .doesNotContain(seatNumber);

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token2)
                        .content(objectMapper.writeValueAsString(new ReservationRequest(seatId))))
                .andExpect(status().isConflict());
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

    private int reserveSeat(String token, CountDownLatch readyLatch, CountDownLatch startLatch) throws Exception {
        readyLatch.countDown();
        startLatch.await();

        return mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(new ReservationRequest(seatId))))
                .andReturn()
                .getResponse()
                .getStatus();
    }
}

