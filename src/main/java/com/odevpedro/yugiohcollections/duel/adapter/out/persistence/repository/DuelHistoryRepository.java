package com.odevpedro.yugiohcollections.duel.adapter.out.persistence.repository;

import com.odevpedro.yugiohcollections.duel.adapter.out.persistence.entity.DuelHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DuelHistoryRepository extends JpaRepository<DuelHistoryEntity, Long> {

    Optional<DuelHistoryEntity> findByDuelId(String duelId);

    List<DuelHistoryEntity> findByPlayerAIdOrPlayerBIdOrderByFinishedAtDesc(String playerAId, String playerBId);

    List<DuelHistoryEntity> findByWinnerIdOrLoserIdOrderByFinishedAtDesc(String winnerId, String loserId);

    List<DuelHistoryEntity> findTop100ByOrderByFinishedAtDesc();
}