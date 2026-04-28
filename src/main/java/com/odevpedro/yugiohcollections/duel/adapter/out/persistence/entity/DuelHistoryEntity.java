package com.odevpedro.yugiohcollections.duel.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "duel_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuelHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "duel_id", nullable = false, unique = true)
    private String duelId;

    @Column(name = "player_a_id", nullable = false)
    private String playerAId;

    @Column(name = "player_b_id", nullable = false)
    private String playerBId;

    @Column(name = "winner_id")
    private String winnerId;

    @Column(name = "loser_id")
    private String loserId;

    @Column(name = "player_a_final_lp")
    private Integer playerAFinalLp;

    @Column(name = "player_b_final_lp")
    private Integer playerBFinalLp;

    @Column(name = "turn_count")
    private Integer turnCount;

    @Column(name = "duel_type")
    private String duelType;

    @Column(name = "result")
    private String result;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "duration_seconds")
    private Long durationSeconds;
}