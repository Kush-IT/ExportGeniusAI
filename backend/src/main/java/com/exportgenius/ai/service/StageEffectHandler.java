package com.exportgenius.ai.service;

import com.exportgenius.ai.entity.*;
import com.exportgenius.ai.repository.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class StageEffectHandler {

    private final MarginLedgerRepository marginLedgerRepository;
    private final PaymentInRepository paymentInRepository;
    private final PaymentOutRepository paymentOutRepository;
    private final RequirementRepository requirementRepository;

    public StageEffectHandler(MarginLedgerRepository marginLedgerRepository,
                              PaymentInRepository paymentInRepository,
                              PaymentOutRepository paymentOutRepository,
                              RequirementRepository requirementRepository) {
        this.marginLedgerRepository = marginLedgerRepository;
        this.paymentInRepository = paymentInRepository;
        this.paymentOutRepository = paymentOutRepository;
        this.requirementRepository = requirementRepository;
    }

    @Transactional
    public void handle(Deal deal, DealStage stage) {
        switch (stage) {
            case SOURCING -> {
                // Side effects for SOURCING stage (e.g. notify admin)
                System.out.println("Deal entered SOURCING stage. Linking requirement and catalogue...");
            }
            case CONFIRMED -> {
                // 1. Create margin_ledger row
                BigDecimal quantityBD = BigDecimal.valueOf(deal.getQuantity());
                BigDecimal marginPerUnit = deal.getSellPrice().subtract(deal.getSupplyPrice());
                BigDecimal totalMarginAmount = marginPerUnit.multiply(quantityBD);

                MarginLedger ledger = MarginLedger.builder()
                        .deal(deal)
                        .marginAmount(totalMarginAmount)
                        .marginPct(deal.getMarginPct())
                        .build();
                marginLedgerRepository.save(ledger);

                // 2. Create payments_in row (PENDING status, total sell price)
                BigDecimal totalInAmount = deal.getSellPrice().multiply(quantityBD);
                PaymentIn paymentIn = PaymentIn.builder()
                        .deal(deal)
                        .amount(totalInAmount)
                        .status("PENDING")
                        .build();
                paymentInRepository.save(paymentIn);

                // 3. Create payments_out row (PENDING status, total supply price)
                BigDecimal totalOutAmount = deal.getSupplyPrice().multiply(quantityBD);
                PaymentOut paymentOut = PaymentOut.builder()
                        .deal(deal)
                        .amount(totalOutAmount)
                        .status("PENDING")
                        .build();
                paymentOutRepository.save(paymentOut);

                System.out.println("Deal CONFIRMED. Ledger and payments tables populated.");
            }
            case PAID -> {
                // 1. Set margin_ledger.captured_at = now()
                marginLedgerRepository.findByDeal(deal).ifPresent(ledger -> {
                    ledger.setCapturedAt(LocalDateTime.now());
                    marginLedgerRepository.save(ledger);
                });

                // 2. Close the linked requirement status
                Requirement requirement = deal.getRequirement();
                requirement.setStatus("CLOSED");
                requirementRepository.save(requirement);

                System.out.println("Deal PAID. Margin ledger finalized and requirement closed.");
            }
            default -> {
                // No special side effects for other stages (QUOTED, NEGOTIATING, DISPATCHED)
            }
        }
    }
}
