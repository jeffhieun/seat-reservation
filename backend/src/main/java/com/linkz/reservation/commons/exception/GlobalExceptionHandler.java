package com.linkz.reservation.commons.exception;

import com.linkz.reservation.reservation.DuplicateReservationException;
import com.linkz.reservation.reservation.InvalidReservationTransitionException;
import com.linkz.reservation.reservation.ReservationErrorResponse;
import com.linkz.reservation.reservation.ReservationNotFoundException;
import com.linkz.reservation.reservation.SeatUnavailableException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        String message = ex.getBindingResult()
            .getAllErrors()
            .stream()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .collect(Collectors.joining(", "));
        
        log.warn("Validation error in request: {}", message);
        
        return ResponseEntity
            .badRequest()
            .body(buildResponse(HttpStatus.BAD_REQUEST, "Validation Error", message, request.getRequestURI()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(buildResponse(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(DuplicateReservationException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateReservationException(
            DuplicateReservationException ex,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(SeatUnavailableException.class)
    public ResponseEntity<ReservationErrorResponse> handleSeatUnavailableException(
            SeatUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ReservationErrorResponse("SEAT_UNAVAILABLE", ex.getMessage()));
    }

    @ExceptionHandler(InvalidReservationTransitionException.class)
    public ResponseEntity<ReservationErrorResponse> handleInvalidReservationTransitionException(
            InvalidReservationTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ReservationErrorResponse("INVALID_RESERVATION_TRANSITION", ex.getMessage()));
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ReservationErrorResponse> handleReservationNotFoundException(
            ReservationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ReservationErrorResponse("RESERVATION_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiErrorResponse> handleJwtException(
            JwtException ex,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid or expired token", request.getRequestURI()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred", ex);
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(buildResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal Server Error",
                    "An unexpected error occurred. Please try again later.",
                    request.getRequestURI()
            ));
    }

    private ApiErrorResponse buildResponse(HttpStatus status, String error, String message, String path) {
        return new ApiErrorResponse(
                Instant.now().toString(),
                status.value(),
                error,
                message,
                path
        );
    }
}
