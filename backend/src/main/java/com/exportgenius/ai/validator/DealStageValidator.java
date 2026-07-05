package com.exportgenius.ai.validator;

import com.exportgenius.ai.entity.*;
import com.exportgenius.ai.exception.IllegalStateTransitionException;
import com.exportgenius.ai.repository.PaymentInRepository;
import com.exportgenius.ai.repository.PaymentOutRepository;
import com.exportgenius.ai.repository.QaReportRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class DealStageValidator {

    private final QaReportRepository qaReportRepository;
    private final PaymentInRepository paymentInRepository;
    private final PaymentOutRepository paymentOutRepository;

    @Value("${deal.minimum-margin-pct:0.15}")
    private double minimumMarginPct;

    public DealStageValidator(QaReportRepository qaReportRepository,
                              PaymentInRepository paymentInRepository,
                              PaymentOutRepository paymentOutRepository) {
        this.qaReportRepository = qaReportRepository;
        this.paymentInRepository = paymentInRepository;
        this.paymentOutRepository = paymentOutRepository;
    }

    public void validate(Deal deal, DealStage target) {
        DealStage current = deal.getStage();

        if (current == target) {
            return; // No-op if transitioning to the same stage
        }

        switch (target) {
            case QUOTED -> {
                // SOURCING -> QUOTED or NEGOTIATING -> QUOTED (bidirectional iteration)
                if (current != DealStage.SOURCING && current != DealStage.NEGOTIATING) {
                    throw new IllegalStateTransitionException("Cannot transition to QUOTED from " + current);
                }
                if (deal.getSellPrice().compareTo(deal.getSupplyPrice()) <= 0) {
                    throw new IllegalStateTransitionException("Positive margin required: Sell price must exceed supply price");
                }
            }
            case NEGOTIATING -> {
                // QUOTED -> NEGOTIATING
                if (current != DealStage.QUOTED) {
                    throw new IllegalStateTransitionException("Cannot transition to NEGOTIATING from " + current);
                }
            }
            case CONFIRMED -> {
                // QUOTED -> CONFIRMED or NEGOTIATING -> CONFIRMED
                if (current != DealStage.QUOTED && current != DealStage.NEGOTIATING) {
                    throw new IllegalStateTransitionException("Cannot transition to CONFIRMED from " + current);
                }
                if (!deal.isImporterAccepted()) {
                    throw new IllegalStateTransitionException("Cannot confirm: Importer has not accepted the quote");
                }
                
                // Margin threshold validation
                BigDecimal minMarginDecimal = BigDecimal.valueOf(minimumMarginPct);
                if (deal.getMarginPct().compareTo(minMarginDecimal) < 0) {
                    throw new IllegalStateTransitionException("Cannot confirm: Margin of " + deal.getMarginPct() 
                            + " is below the required minimum threshold of " + minMarginDecimal);
                }

                // QA Report check: A passing QA report must exist for the catalogue item
                Optional<QaReport> passingReport = qaReportRepository
                        .findFirstByCatalogueAndPassedTrueOrderByCreatedAtDesc(deal.getCatalogue());
                if (passingReport.isEmpty()) {
                    throw new IllegalStateTransitionException("Cannot confirm: No passing QA report exists for catalog item");
                }

                // Trade Compliance check placeholder (Will check HS codes and destination country)
                validateTradeCompliance(deal);
            }
            case DISPATCHED -> {
                // CONFIRMED -> DISPATCHED
                if (current != DealStage.CONFIRMED) {
                    throw new IllegalStateTransitionException("Cannot transition to DISPATCHED from " + current);
                }
                if (deal.getShippingDocumentUrl() == null || deal.getShippingDocumentUrl().trim().isEmpty()) {
                    throw new IllegalStateTransitionException("Cannot dispatch: Shipping documentation is missing");
                }

                // Check that payment from the importer has been received
                PaymentIn paymentIn = paymentInRepository.findFirstByDealOrderByCreatedAtDesc(deal)
                        .orElseThrow(() -> new IllegalStateTransitionException("Cannot dispatch: PaymentIn record not found"));
                if (!paymentIn.getStatus().equals("RECEIVED")) {
                    throw new IllegalStateTransitionException("Cannot dispatch: Importer payment has not been received (Current status: " + paymentIn.getStatus() + ")");
                }
            }
            case PAID -> {
                // DISPATCHED -> PAID
                if (current != DealStage.DISPATCHED) {
                    throw new IllegalStateTransitionException("Cannot transition to PAID from " + current);
                }

                // Verify both payments_in is RECEIVED and payments_out is SETTLED
                PaymentIn paymentIn = paymentInRepository.findFirstByDealOrderByCreatedAtDesc(deal)
                        .orElseThrow(() -> new IllegalStateTransitionException("Cannot pay: PaymentIn record not found"));
                PaymentOut paymentOut = paymentOutRepository.findFirstByDealOrderByCreatedAtDesc(deal)
                        .orElseThrow(() -> new IllegalStateTransitionException("Cannot pay: PaymentOut record not found"));

                if (!paymentIn.getStatus().equals("RECEIVED")) {
                    throw new IllegalStateTransitionException("Cannot pay: Importer payment must be RECEIVED");
                }
                if (!paymentOut.getStatus().equals("SETTLED")) {
                    throw new IllegalStateTransitionException("Cannot pay: Exporter payout must be SETTLED");
                }
            }
            default -> throw new IllegalStateTransitionException("Invalid state transition target: " + target);
        }
    }

    private void validateTradeCompliance(Deal deal) {
        String hsCode = deal.getCatalogue().getHsCode();
        String country = deal.getRequirement().getDestinationCountry();
        
        // Simple mock rule check: HS codes starting with restricted prefix for country
        // e.g., if country is "Germany" and HS Code starts with "999", raise compliance error
        if (hsCode != null && hsCode.startsWith("999")) {
            throw new IllegalStateTransitionException("Trade compliance block: HS Code " + hsCode 
                    + " is restricted for destination country " + country);
        }
    }
}
