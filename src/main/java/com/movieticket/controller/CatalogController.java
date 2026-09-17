package com.movieticket.controller;

import com.movieticket.entity.City;
import com.movieticket.entity.Seat;
import com.movieticket.entity.Show;
import com.movieticket.entity.Theater;
import com.movieticket.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public browse APIs (no login required).
 */
@RestController
@RequiredArgsConstructor
public class CatalogController {

    private final AdminService adminService;

    @GetMapping("/api/cities")
    public List<City> cities() {
        return adminService.listCities();
    }

    @GetMapping("/api/theaters")
    public List<Theater> theaters(@RequestParam(required = false) Long cityId) {
        return adminService.listTheaters(cityId);
    }

    @GetMapping("/api/theaters/{id}")
    public Theater theater(@PathVariable Long id) {
        return adminService.getTheater(id);
    }

    @GetMapping("/api/theaters/{id}/seats")
    public List<Seat> seats(@PathVariable Long id) {
        return adminService.getSeats(id);
    }

    @GetMapping("/api/shows")
    public List<Show> shows(@RequestParam(required = false) Long cityId,
                            @RequestParam(required = false) Long theaterId) {
        return adminService.listShows(cityId, theaterId);
    }

    @GetMapping("/api/shows/{id}")
    public Show show(@PathVariable Long id) {
        return adminService.getShow(id);
    }
}
