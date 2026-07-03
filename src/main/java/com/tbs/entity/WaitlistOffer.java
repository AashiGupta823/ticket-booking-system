package com.tbs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "waitlist_offers")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitlistOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waitlist_id", nullable = false, unique = true)
    @JsonIgnore
    private Waitlist waitlistEntry;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_seat_id", nullable = false, unique = true)
    private ShowSeat showSeat;

    @Column(nullable = false, unique = true)
    private String offerToken;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    public enum Status {
        PENDING, ACCEPTED, EXPIRED
    }
}