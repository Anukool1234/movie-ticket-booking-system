package com.movieticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Movie Ticket Booking System.
 *
 * EnableScheduling → runs background jobs (e.g. release expired seat holds)
 * EnableAsync      → sends notifications without blocking the booking API
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class MovieTicketBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(MovieTicketBookingApplication.class, args);
    }
}
