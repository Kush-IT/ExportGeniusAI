package com.exportgenius.ai.repository;

import com.exportgenius.ai.entity.Deal;
import com.exportgenius.ai.entity.MarginLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarginLedgerRepository extends JpaRepository<MarginLedger, UUID> {
    Optional<MarginLedger> findByDeal(Deal deal);

    @org.springframework.data.jpa.repository.Query("SELECT AVG(m.marginPct) FROM MarginLedger m WHERE m.deal.catalogue.category.name = :categoryName AND m.createdAt >= :since")
    Double getAverageMarginPctByCategoryAndDate(@org.springframework.data.repository.query.Param("categoryName") String categoryName, @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);
}
