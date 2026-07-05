package com.exportgenius.ai.repository;

import com.exportgenius.ai.entity.Deal;
import com.exportgenius.ai.entity.PaymentOut;
import com.exportgenius.ai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentOutRepository extends JpaRepository<PaymentOut, UUID> {
    List<PaymentOut> findByDeal(Deal deal);
    Optional<PaymentOut> findFirstByDealOrderByCreatedAtDesc(Deal deal);
    List<Deal> findByDealCatalogueExporter(User exporter); // Wait, finding by exporter
    List<PaymentOut> findByDealCatalogueExporterId(UUID exporterId);
}
