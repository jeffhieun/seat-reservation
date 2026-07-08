package com.linkz.reservation.reservation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ReservationController.class)
public class ReservationExceptionHandler {

    @ExceptionHandler(DuplicateReservationException.class)
    public ResponseEntity<ReservationErrorResponse> handleDuplicateReservation(DuplicateReservationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ReservationErrorResponse("DUPLICATE_RESERVATION", ex.getMessage()));
    }

    @ExceptionHandler(SeatUnavailableException.class)
    public ResponseEntity<ReservationErrorResponse> handleSeatUnavailable(SeatUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ReservationErrorResponse("SEAT_UNAVAILABLE", ex.getMessage()));
    }

    @ExceptionHandler(InvalidReservationTransitionException.class)
    public ResponseEntity<ReservationErrorResponse> handleInvalidTransition(InvalidReservationTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ReservationErrorResponse("INVALID_RESERVATION_TRANSITION", ex.getMessage()));
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ReservationErrorResponse> handleReservationNotFound(ReservationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ReservationErrorResponse("RESERVATION_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ReservationErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ReservationErrorResponse("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ReservationErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ReservationErrorResponse("FORBIDDEN", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ReservationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid request")
                .orElse("Invalid request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ReservationErrorResponse("BAD_REQUEST", message));
    }
}

