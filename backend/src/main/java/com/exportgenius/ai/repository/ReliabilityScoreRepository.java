package com.exportgenius.ai.repository;

import com.exportgenius.ai.entity.ReliabilityScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReliabilityScoreRepository extends JpaRepository<ReliabilityScore, UUID> {
}
