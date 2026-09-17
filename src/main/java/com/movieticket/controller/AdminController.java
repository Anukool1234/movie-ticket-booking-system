package com.movieticket.controller;

import com.movieticket.dto.*;
import com.movieticket.entity.*;
import com.movieticket.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin-only APIs for managing catalog and policies.
 * Protected by hasRole('ADMIN') in SecurityConfig.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/cities")
    @ResponseStatus(HttpStatus.CREATED)
    public City createCity(@Valid @RequestBody CityRequest request) {
        return adminService.createCity(request);
    }

    @PostMapping("/theaters")
    @ResponseStatus(HttpStatus.CREATED)
    public Theater createTheater(@Valid @RequestBody TheaterRequest request) {
        return adminService.createTheater(request);
    }

    @PostMapping("/theaters/{theaterId}/seats")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Seat> createSeatLayout(@PathVariable Long theaterId,
                                       @Valid @RequestBody SeatLayoutRequest request) {
        return adminService.createSeatLayout(theaterId, request);
    }

    @PostMapping("/shows")
    @ResponseStatus(HttpStatus.CREATED)
    public Show createShow(@Valid @RequestBody ShowRequest request) {
        return adminService.createShow(request);
    }

    @PostMapping("/discounts")
    @ResponseStatus(HttpStatus.CREATED)
    public DiscountCode createDiscount(@Valid @RequestBody DiscountCodeRequest request) {
        return adminService.createDiscount(request);
    }

    @GetMapping("/discounts")
    public List<DiscountCode> listDiscounts() {
        return adminService.listDiscounts();
    }

    @PostMapping("/pricing-tiers")
    public PricingTier upsertPricing(@Valid @RequestBody PricingTierRequest request) {
        return adminService.upsertPricingTier(request);
    }

    @GetMapping("/pricing-tiers")
    public List<PricingTier> listPricing() {
        return adminService.listPricingTiers();
    }

    @PostMapping("/refund-policies")
    @ResponseStatus(HttpStatus.CREATED)
    public RefundPolicy createRefundPolicy(@Valid @RequestBody RefundPolicyRequest request) {
        return adminService.createRefundPolicy(request);
    }

    @GetMapping("/refund-policies")
    public List<RefundPolicy> listRefundPolicies() {
        return adminService.listRefundPolicies();
    }
}
