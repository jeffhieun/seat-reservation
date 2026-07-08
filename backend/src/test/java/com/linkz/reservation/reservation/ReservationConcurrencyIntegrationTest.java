package com.linkz.reservation.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkz.reservation.audit.AuditEventRepository;
import com.linkz.reservation.auth.LoginRequest;
import com.linkz.reservation.auth.LoginResponse;
import com.linkz.reservation.payment.PaymentRepository;
import com.linkz.reservation.seat.Seat;
import com.linkz.reservation.seat.SeatRepository;
import com.linkz.reservation.seat.SeatStatus;
import com.linkz.reservation.user.User;
import com.linkz.reservation.user.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationConcurrencyIntegrationTest {

    private static final PostgresTestDatabase POSTGRES = new PostgresTestDatabase();

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", POSTGRES::getDdlAuto);
        registry.add("spring.jpa.properties.hibernate.dialect", POSTGRES::getDialect);
        registry.add("spring.liquibase.enabled", POSTGRES::isLiquibaseEnabled);
    }

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
    private AuditEventRepository auditEventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
        userRepository.deleteAll();
        auditEventRepository.deleteAll();
    }

    @AfterAll
    static void tearDown() {
        POSTGRES.stop();
    }

    @Test
    void sameSeatShouldAllowOnlyOneConcurrentReservation() throws Exception {
        Seat seat = createSeat("CC-1", SeatStatus.AVAILABLE);
        String firstToken = login(createUser("cc1-user1@test.com").getEmail());
        String secondToken = login(createUser("cc1-user2@test.com").getEmail());

        List<Integer> statuses = runConcurrentReservations(
                List.of(
                        reservationAttempt(firstToken, seat.getId()),
                        reservationAttempt(secondToken, seat.getId())
                )
        );

        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(reservationRepository.count()).isEqualTo(1);
        assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus()).isEqualTo(SeatStatus.PENDING_PAYMENT);
    }

    @Test
    void tenConcurrentUsersShouldCreateOnlyOneReservationForSameSeat() throws Exception {
        Seat seat = createSeat("CC-2", SeatStatus.AVAILABLE);
        List<Callable<Integer>> attempts = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            String email = "cc2-user-" + i + "@test.com";
            String token = login(createUser(email).getEmail());
            attempts.add(reservationAttempt(token, seat.getId()));
        }

        List<Integer> statuses = runConcurrentReservations(attempts);

        assertThat(statuses).containsExactlyInAnyOrder(201, 409, 409, 409, 409, 409, 409, 409, 409, 409);
        assertThat(reservationRepository.count()).isEqualTo(1);
        assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus()).isEqualTo(SeatStatus.PENDING_PAYMENT);
    }

    @Test
    void concurrentReservationsOnDifferentSeatsShouldAllSucceed() throws Exception {
        List<Callable<Integer>> attempts = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            Seat seat = createSeat("CC-3-" + i, SeatStatus.AVAILABLE);
            String token = login(createUser("cc3-user-" + i + "@test.com").getEmail());
            attempts.add(reservationAttempt(token, seat.getId()));
        }

        List<Integer> statuses = runConcurrentReservations(attempts);

        assertThat(statuses).allMatch(status -> status == 201);
        assertThat(reservationRepository.count()).isEqualTo(4);
        assertThat(seatRepository.findAll()).allMatch(seat -> seat.getStatus() == SeatStatus.PENDING_PAYMENT);
    }

    @Test
    void confirmedSeatShouldRejectConcurrentReservations() throws Exception {
        Seat seat = createSeat("CC-4", SeatStatus.AVAILABLE);
        User owner = createUser("cc4-owner@test.com");
        Reservation reservation = reservationService.reserveSeat(owner.getId(), seat.getId());
        reservationService.confirmReservation(reservation.getId(), owner.getId());

        List<Callable<Integer>> attempts = List.of(
                reservationAttempt(login(createUser("cc4-user-1@test.com").getEmail()), seat.getId()),
                reservationAttempt(login(createUser("cc4-user-2@test.com").getEmail()), seat.getId())
        );

        List<Integer> statuses = runConcurrentReservations(attempts);

        assertThat(statuses).allMatch(status -> status == 409);
        assertThat(reservationRepository.count()).isEqualTo(1);
        assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus()).isEqualTo(SeatStatus.RESERVED);
    }

    @Test
    void expiredReservationShouldNotBlockNewConcurrentReservation() throws Exception {
        Seat seat = createSeat("CC-5", SeatStatus.AVAILABLE);
        User owner = createUser("cc5-owner@test.com");
        Reservation reservation = reservationService.reserveSeat(owner.getId(), seat.getId());
        reservationService.expireReservation(reservation.getId(), owner.getId());

        List<Callable<Integer>> attempts = List.of(
                reservationAttempt(login(createUser("cc5-user-1@test.com").getEmail()), seat.getId()),
                reservationAttempt(login(createUser("cc5-user-2@test.com").getEmail()), seat.getId())
        );

        List<Integer> statuses = runConcurrentReservations(attempts);

        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(reservationRepository.count()).isEqualTo(2);
        assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus()).isEqualTo(SeatStatus.PENDING_PAYMENT);
        assertThat(reservationRepository.findByUserIdOrderByCreatedAtDesc(owner.getId()))
                .last()
                .extracting(Reservation::getStatus)
                .isEqualTo(ReservationStatus.EXPIRED);
    }

    private Callable<Integer> reservationAttempt(String token, Long seatId) {
        return () -> mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("seatId", seatId))))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private List<Integer> runConcurrentReservations(List<Callable<Integer>> attempts) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(attempts.size());
        CountDownLatch ready = new CountDownLatch(attempts.size());
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<CompletableFuture<Integer>> futures = attempts.stream()
                    .map(attempt -> CompletableFuture.supplyAsync(() -> {
                        ready.countDown();
                        try {
                            if (!start.await(30, TimeUnit.SECONDS)) {
                                throw new IllegalStateException("Timed out waiting to start concurrent reservation");
                            }
                            return attempt.call();
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }, executor))
                    .toList();

            assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Integer> statuses = new ArrayList<>();
            for (CompletableFuture<Integer> future : futures) {
                statuses.add(future.get(60, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            executor.shutdownNow();
        }
    }

    private User createUser(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("password123"))
                .build());
    }

    private Seat createSeat(String seatNumber, SeatStatus status) {
        return seatRepository.save(Seat.builder()
                .seatNumber(seatNumber)
                .status(status)
                .build());
    }

    private String login(String email) throws Exception {
        String responseBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "password123"))))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(responseBody, LoginResponse.class).token();
    }

    private static final class PostgresTestDatabase {
        private final String containerName = "seat-reservation-concurrency-" + UUID.randomUUID();
        private final int hostPort = findFreePort();
        private boolean started;

        void start() {
            if (started) {
                return;
            }

            runCommand(List.of(
                    "podman", "rm", "-f", containerName
            ), true);

            runCommand(List.of(
                    "podman", "run", "-d",
                    "--name", containerName,
                    "-e", "POSTGRES_USER=seat_reservation",
                    "-e", "POSTGRES_PASSWORD=seat_reservation",
                    "-e", "POSTGRES_DB=seat_reservation_test",
                    "-p", hostPort + ":5432",
                    "postgres:16-alpine"
            ), false);

            waitForDatabase();
            started = true;
        }

        void stop() {
            if (!started) {
                return;
            }
            runCommand(List.of("podman", "rm", "-f", containerName), true);
            started = false;
        }

        String getJdbcUrl() {
            return "jdbc:postgresql://127.0.0.1:" + hostPort + "/seat_reservation_test";
        }

        String getUsername() {
            return "seat_reservation";
        }

        String getPassword() {
            return "seat_reservation";
        }

        String getDriverClassName() {
            return "org.postgresql.Driver";
        }

        String getDdlAuto() {
            return "none";
        }

        String getDialect() {
            return "org.hibernate.dialect.PostgreSQLDialect";
        }

        boolean isLiquibaseEnabled() {
            return true;
        }

        private void waitForDatabase() {
            long deadline = System.nanoTime() + Duration.ofSeconds(90).toNanos();
            Throwable lastFailure = null;

            while (System.nanoTime() < deadline) {
                try (var connection = DriverManager.getConnection(getJdbcUrl(), getUsername(), getPassword())) {
                    return;
                } catch (SQLException ex) {
                    lastFailure = ex;
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for PostgreSQL test container", ex);
                }
            }

            throw new IllegalStateException("PostgreSQL test container did not become ready", lastFailure);
        }

        private static int findFreePort() {
            try (ServerSocket socket = new ServerSocket(0)) {
                socket.setReuseAddress(true);
                return socket.getLocalPort();
            } catch (IOException ex) {
                throw new IllegalStateException("Unable to allocate free port", ex);
            }
        }

        private static void runCommand(List<String> command, boolean ignoreFailure) {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            try {
                Process process = builder.start();
                String output;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    output = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                }
                int exitCode = process.waitFor();
                if (exitCode != 0 && !ignoreFailure) {
                    throw new IllegalStateException("Command failed: " + String.join(" ", command) + System.lineSeparator() + output);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Failed to run command: " + String.join(" ", command), ex);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to run command: " + String.join(" ", command), ex);
            }
        }
    }
}
