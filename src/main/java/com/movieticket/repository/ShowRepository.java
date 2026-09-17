package com.movieticket.repository;

import com.movieticket.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {
    List<Show> findByTheaterId(Long theaterId);
    List<Show> findByTheaterCityId(Long cityId);
}
