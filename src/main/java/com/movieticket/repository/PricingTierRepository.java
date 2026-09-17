package com.movieticket.repository;

import com.movieticket.entity.PricingTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PricingTierRepository extends JpaRepository<PricingTier, Long> {
    Optional<PricingTier> findByName(String name);
}
