package com.movieticket.entity;

import com.movieticket.entity.enums.SeatStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Per-show seat availability state.
 *
 * Why separate from Seat?
 * Seat = physical layout of theater (shared across shows)
 * ShowSeat = booking state of that seat for ONE show
 *
 * version field enables optimistic locking so two users
 * cannot book the same seat at the same time.
 */
@Entity
@Table(name = "show_seats", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"show_id", "seat_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "show_id")
    private Show show;

    @ManyToOne(optional = false)
    @JoinColumn(name = "seat_id")
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    /** Who currently holds this seat (nullable when AVAILABLE) */
    @ManyToOne
    @JoinColumn(name = "held_by_user_id")
    private User heldBy;

    /** Hold expires at this time; scheduler releases after expiry */
    private LocalDateTime holdExpiresAt;

    /**
     * Optimistic lock version — if two transactions update the same row,
     * one fails with OptimisticLockException (no double booking).
     */
    @Version
    private Long version;
}
