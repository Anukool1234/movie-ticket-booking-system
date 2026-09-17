package com.movieticket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for the core booking flow:
 * browse seats → hold → pay → history → cancel.
 *
 * <p>Protected requests use HTTP Basic credentials. This verifies both authentication
 * and the ADMIN/CUSTOMER access rules without a separate login/token flow.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookingFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullBookingAndCancelFlow() throws Exception {
        // 1) List shows (public)
        MvcResult showsResult = mockMvc.perform(get("/api/shows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andReturn();

        long showId = objectMapper.readTree(showsResult.getResponse().getContentAsString())
                .get(0).get("id").asLong();

        // 2) Get available seats
        MvcResult seatsResult = mockMvc.perform(get("/api/shows/" + showId + "/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andReturn();

        JsonNode seats = objectMapper.readTree(seatsResult.getResponse().getContentAsString());
        long seat1 = -1;
        long seat2 = -1;
        for (JsonNode seat : seats) {
            if ("AVAILABLE".equals(seat.get("status").asText())) {
                if (seat1 < 0) {
                    seat1 = seat.get("showSeatId").asLong();
                } else {
                    seat2 = seat.get("showSeatId").asLong();
                    break;
                }
            }
        }
        if (seat1 < 0 || seat2 < 0) {
            throw new IllegalStateException("Need at least 2 available seats for this test");
        }

        // 3) Create booking (hold seats) with discount
        String createBody = """
                {"showId": %d, "showSeatIds": [%d, %d], "discountCode": "SAVE10"}
                """.formatted(showId, seat1, seat2);

        MvcResult bookingResult = mockMvc.perform(post("/api/bookings")
                        .with(httpBasic("user@movie.com", "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.discountCodeUsed").value("SAVE10"))
                .andExpect(jsonPath("$.seatLabels", hasSize(2)))
                .andReturn();

        long bookingId = objectMapper.readTree(bookingResult.getResponse().getContentAsString())
                .get("id").asLong();

        // 4) Pay
        mockMvc.perform(post("/api/bookings/pay")
                        .with(httpBasic("user@movie.com", "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\": " + bookingId + ", \"success\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));

        // 5) History
        mockMvc.perform(get("/api/bookings")
                        .with(httpBasic("user@movie.com", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(bookingId));

        // 6) Cancel → refund
        mockMvc.perform(post("/api/bookings/" + bookingId + "/cancel")
                        .with(httpBasic("user@movie.com", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundAmount", notNullValue()));
    }

    @Test
    void adminCanCreateCityButCustomerCannot() throws Exception {
        mockMvc.perform(post("/api/admin/cities")
                        .with(httpBasic("admin@movie.com", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Bangalore\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Bangalore"));

        mockMvc.perform(post("/api/admin/cities")
                        .with(httpBasic("user@movie.com", "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Chennai\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotDoubleBookSameSeat() throws Exception {
        MvcResult showsResult = mockMvc.perform(get("/api/shows")).andReturn();
        long showId = objectMapper.readTree(showsResult.getResponse().getContentAsString())
                .get(0).get("id").asLong();

        MvcResult seatsResult = mockMvc.perform(get("/api/shows/" + showId + "/seats")).andReturn();
        JsonNode seats = objectMapper.readTree(seatsResult.getResponse().getContentAsString());
        long seatId = -1;
        for (JsonNode seat : seats) {
            if ("AVAILABLE".equals(seat.get("status").asText())) {
                seatId = seat.get("showSeatId").asLong();
                break;
            }
        }
        if (seatId < 0) {
            throw new IllegalStateException("Need an available seat for this test");
        }

        String body = "{\"showId\": " + showId + ", \"showSeatIds\": [" + seatId + "]}";

        mockMvc.perform(post("/api/bookings")
                        .with(httpBasic("user@movie.com", "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Second attempt on same seat should fail
        mockMvc.perform(post("/api/bookings")
                        .with(httpBasic("user@movie.com", "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

}
