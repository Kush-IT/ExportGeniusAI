package com.exportgenius.ai.repository;

import com.exportgenius.ai.entity.Deal;
import com.exportgenius.ai.entity.PaymentIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentInRepository extends JpaRepository<PaymentIn, UUID> {
    List<PaymentIn> findByDeal(Deal deal);
    Optional<PaymentIn> findFirstByDealOrderByCreatedAtDesc(Deal deal);
}
