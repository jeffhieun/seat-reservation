package com.linkz.reservation.reservation;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "reservation")
@Getter
@Setter
public class ReservationProperties {
    
    /**
     * Number of minutes a reservation is valid before auto-expiration.
     * Default: 10 minutes
     */
    private int expirationMinutes = 10;
    
    /**
     * Scheduler interval in seconds for checking expired reservations.
     * Default: 60 seconds
     */
    private int schedulerIntervalSeconds = 60;
}


