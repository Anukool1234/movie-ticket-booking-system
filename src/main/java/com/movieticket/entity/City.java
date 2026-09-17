package com.movieticket.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * City where theaters operate. Example: Mumbai, Delhi.
 */
@Entity
@Table(name = "cities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}
