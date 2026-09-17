package com.movieticket.repository;

import com.movieticket.entity.RefundPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundPolicyRepository extends JpaRepository<RefundPolicy, Long> {
    List<RefundPolicy> findByActiveTrueOrderByMinHoursBeforeShowDesc();
}
