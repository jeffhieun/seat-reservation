package com.linkz.reservation.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {
    
    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;

    @Value("${webhook.secret:dev-secret}")
    private String webhookSecret;
    
    @PostMapping("/payment-success")
    public ResponseEntity<Void> handlePaymentSuccess(
            Authentication authentication,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestBody Map<String, Object> body) {
        try {
            String eventId = (String) body.get("eventId");
            String providerReference = (String) body.get("providerReference");
            
            log.debug("Processing payment success webhook: {}", eventId);

            if (signature != null && !signature.isBlank()) {
                if (!verifySignature(signature, body)) {
                    log.warn("Invalid webhook signature for event {}", eventId);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
                log.debug("Webhook signature verified for event {}", eventId);
            }

            webhookService.processPaymentSuccess(eventId, providerReference);

            log.info("Payment success webhook processed: {}", eventId);

            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Webhook processing failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Unexpected error processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/payment-failure")
    public ResponseEntity<Void> handlePaymentFailure(
            Authentication authentication,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestBody Map<String, Object> body) {
        try {
            String eventId = (String) body.get("eventId");
            String providerReference = (String) body.get("providerReference");
            
            log.debug("Processing payment failure webhook: {}", eventId);

            if (signature != null && !signature.isBlank()) {
                if (!verifySignature(signature, body)) {
                    log.warn("Invalid webhook signature for event {}", eventId);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
                log.debug("Webhook signature verified for event {}", eventId);
            }

            webhookService.processPaymentFailure(eventId, providerReference);

            log.info("Payment failure webhook processed: {}", eventId);

            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Webhook processing failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Unexpected error processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private boolean verifySignature(String signatureHeader, Map<String, Object> body) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }

        try {
            String payload = objectMapper.writeValueAsString(body);
            String expected = hmacSha256Hex(webhookSecret, payload);

            byte[] a = expected.getBytes(StandardCharsets.UTF_8);
            byte[] b = signatureHeader.getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(a, b);
        } catch (Exception e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }

    private String hmacSha256Hex(String key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(raw);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}

