package com.movieticket.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Theater belongs to one city and has many seats (layout).
 */
@Entity
@Table(name = "theaters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Theater {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String address;

    @ManyToOne(optional = false)
    @JoinColumn(name = "city_id")
    private City city;
}
