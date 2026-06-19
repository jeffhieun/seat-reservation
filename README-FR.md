# Functional Requirements

## FR-1 User Authentication

- Users can authenticate and access protected resources.
- User sessions remain valid for 90 days after successful authentication.

## FR-2 View Available Seats

- Users can view the list of available seats.
- The system initially provides 3 seats available for reservation.

## FR-3 Select Seat

- Authenticated users can select an available seat for reservation.

## FR-4 Temporary Seat Reservation

- The selected seat is temporarily reserved while payment is in progress.
- Temporarily reserved seats cannot be selected by other users.

## FR-5 Payment Processing

- Users can initiate payment for a selected seat.
- The system integrates with a mock payment provider.

## FR-6 Reservation Confirmation

- The system confirms the reservation after successful payment.
- The seat status is updated accordingly.

## FR-7 Reservation Expiration

- Temporary reservations automatically expire after a configured timeout period.
- Expired seats become available for reservation again.

## FR-8 Concurrent Reservation Handling

- The system prevents multiple users from successfully reserving the same seat simultaneously.

## FR-9 Payment Webhook Processing

- The system receives payment status notifications from the payment provider.
- Payment notifications update reservation and payment statuses.

## FR-10 Audit Tracking

- The system records reservation and payment events for operational visibility.