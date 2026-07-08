package com.linkz.reservation.database;

import com.linkz.reservation.audit.AuditEvent;
import com.linkz.reservation.audit.AuditEventRepository;
import com.linkz.reservation.payment.Payment;
import com.linkz.reservation.payment.PaymentRepository;
import com.linkz.reservation.payment.PaymentStatus;
import com.linkz.reservation.reservation.Reservation;
import com.linkz.reservation.reservation.ReservationRepository;
import com.linkz.reservation.reservation.ReservationStatus;
import com.linkz.reservation.seat.Seat;
import com.linkz.reservation.seat.SeatRepository;
import com.linkz.reservation.seat.SeatStatus;
import com.linkz.reservation.user.User;
import com.linkz.reservation.user.UserRepository;
import com.linkz.reservation.webhook.WebhookEvent;
import com.linkz.reservation.webhook.WebhookEventRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class LiquibasePostgresIntegrationTest {

    private static final PostgresContainer POSTGRES = new PostgresContainer();

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::jdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::username);
        registry.add("spring.datasource.password", POSTGRES::password);
        registry.add("spring.datasource.driver-class-name", POSTGRES::driverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.liquibase.enabled", () -> true);
        registry.add("spring.liquibase.change-log", () -> "classpath:db/changelog/db-changelog-master.xml");
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterAll
    static void stopDatabase() {
        POSTGRES.stop();
    }

    @Test
    void liquibaseAndHibernateShouldLoadOnPostgresAndAllowInserts() {
        assertThat(countTables()).isGreaterThanOrEqualTo(6);
        assertThat(countSequences()).isEqualTo(6);

        User user = userRepository.saveAndFlush(User.builder()
                .email("liquibase-test-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .build());
        Seat seat = seatRepository.saveAndFlush(Seat.builder()
                .seatNumber("LX-" + UUID.randomUUID().toString().substring(0, 4))
                .status(SeatStatus.AVAILABLE)
                .build());

        Reservation reservation = reservationRepository.saveAndFlush(Reservation.builder()
                .user(user)
                .seat(seat)
                .status(ReservationStatus.PENDING_PAYMENT)
                .build());

        Payment payment = paymentRepository.saveAndFlush(Payment.builder()
                .reservation(reservation)
                .status(PaymentStatus.PENDING)
                .amount(new java.math.BigDecimal("10.00"))
                .providerReference("PAY_" + UUID.randomUUID().toString().substring(0, 8))
                .build());

        WebhookEvent webhookEvent = webhookEventRepository.saveAndFlush(WebhookEvent.builder()
                .eventId("evt-" + UUID.randomUUID())
                .build());

        AuditEvent auditEvent = auditEventRepository.saveAndFlush(AuditEvent.builder()
                .eventType(com.linkz.reservation.audit.AuditEventType.PAYMENT_SUCCESS)
                .paymentId(payment.getId())
                .reservationId(reservation.getId())
                .userId(user.getId())
                .seatId(seat.getId())
                .referenceId(webhookEvent.getEventId())
                .details("ok")
                .build());

        assertThat(user.getId()).isNotNull();
        assertThat(seat.getId()).isNotNull();
        assertThat(reservation.getId()).isNotNull();
        assertThat(payment.getId()).isNotNull();
        assertThat(webhookEvent.getId()).isNotNull();
        assertThat(auditEvent.getId()).isNotNull();
    }

    private long countTables() {
        return jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = current_schema()
                  and table_name in ('users', 'seats', 'reservations', 'payments', 'webhook_events', 'audit_events')
                """, Long.class);
    }

    private long countSequences() {
        return jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.sequences
                where sequence_schema = current_schema()
                  and sequence_name in (
                    'users_seq',
                    'seats_seq',
                    'reservations_seq',
                    'payments_seq',
                    'webhook_events_seq',
                    'audit_events_seq'
                  )
                """, Long.class);
    }

    private static final class PostgresContainer {
        private final String containerName = "seat-reservation-liquibase-" + UUID.randomUUID();
        private final int hostPort = findFreePort();
        private boolean started;

        void start() {
            if (started) {
                return;
            }

            run(List.of("podman", "rm", "-f", containerName), true);
            run(List.of(
                    "podman", "run", "-d",
                    "--name", containerName,
                    "-e", "POSTGRES_USER=seat_reservation",
                    "-e", "POSTGRES_PASSWORD=seat_reservation",
                    "-e", "POSTGRES_DB=seat_reservation_test",
                    "-p", hostPort + ":5432",
                    "postgres:16-alpine"
            ), false);
            waitUntilReady();
            started = true;
        }

        void stop() {
            if (!started) {
                return;
            }
            run(List.of("podman", "rm", "-f", containerName), true);
            started = false;
        }

        String jdbcUrl() {
            return "jdbc:postgresql://127.0.0.1:" + hostPort + "/seat_reservation_test";
        }

        String username() {
            return "seat_reservation";
        }

        String password() {
            return "seat_reservation";
        }

        String driverClassName() {
            return "org.postgresql.Driver";
        }

        private void waitUntilReady() {
            long deadline = System.nanoTime() + Duration.ofSeconds(90).toNanos();
            SQLException lastError = null;

            while (System.nanoTime() < deadline) {
                try (var connection = DriverManager.getConnection(jdbcUrl(), username(), password())) {
                    return;
                } catch (SQLException ex) {
                    lastError = ex;
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for PostgreSQL", ex);
                }
            }

            throw new IllegalStateException("PostgreSQL container did not become ready", lastError);
        }

        private static int findFreePort() {
            try (ServerSocket socket = new ServerSocket(0)) {
                socket.setReuseAddress(true);
                return socket.getLocalPort();
            } catch (IOException ex) {
                throw new IllegalStateException("Unable to allocate a free port", ex);
            }
        }

        private static void run(List<String> command, boolean ignoreFailure) {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            try {
                Process process = builder.start();
                String output = new String(process.getInputStream().readAllBytes());
                int exitCode = process.waitFor();
                if (exitCode != 0 && !ignoreFailure) {
                    throw new IllegalStateException(String.join(" ", command) + System.lineSeparator() + output);
                }
            } catch (IOException | InterruptedException ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new IllegalStateException("Failed to run command: " + String.join(" ", command), ex);
            }
        }
    }
}
