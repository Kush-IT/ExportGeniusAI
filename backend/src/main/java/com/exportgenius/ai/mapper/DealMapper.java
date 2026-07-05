package com.exportgenius.ai.mapper;

import com.exportgenius.ai.dto.admin.AdminDealDTO;
import com.exportgenius.ai.dto.exporter.ExporterDealDTO;
import com.exportgenius.ai.dto.importer.ImporterDealDTO;
import com.exportgenius.ai.entity.Deal;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class DealMapper {

    public ExporterDealDTO toExporterDTO(Deal deal) {
        if (deal == null) {
            return null;
        }

        BigDecimal quantityBD = BigDecimal.valueOf(deal.getQuantity());
        BigDecimal paymentFromUs = deal.getSupplyPrice().multiply(quantityBD);

        return ExporterDealDTO.builder()
                .dealId(deal.getId())
                .productTitle(deal.getCatalogue().getTitle())
                .quantity(deal.getQuantity())
                .supplyPrice(deal.getSupplyPrice())
                .paymentFromUs(paymentFromUs)
                .deliveryDeadline(deal.getDeliveryDeadline())
                .stage(deal.getStage())
                .build();
    }

    public ImporterDealDTO toImporterDTO(Deal deal) {
        if (deal == null) {
            return null;
        }

        // Documents list is empty or can be populated from database later
        List<String> documentsAvailable = new ArrayList<>();
        if (deal.getShippingDocumentUrl() != null && !deal.getShippingDocumentUrl().isEmpty()) {
            documentsAvailable.add("SHIPPING_DOCUMENT");
        }

        return ImporterDealDTO.builder()
                .dealId(deal.getId())
                .productTitle(deal.getCatalogue().getTitle())
                .quantity(deal.getQuantity())
                .sellPrice(deal.getSellPrice())
                .stage(deal.getStage())
                .deliveryTimeline(deal.getRequirement().getTimeline())
                .documentsAvailable(documentsAvailable)
                .build();
    }

    public AdminDealDTO toAdminDTO(Deal deal) {
        if (deal == null) {
            return null;
        }

        String exporterName = deal.getCatalogue().getExporter().getFullName();
        String exporterCountry = ""; // Can be retrieved from company profile
        String importerName = deal.getRequirement().getImporter().getFullName();
        String importerCountry = deal.getRequirement().getDestinationCountry();

        return AdminDealDTO.builder()
                .dealId(deal.getId())
                .requirementId(deal.getRequirement().getId())
                .catalogueId(deal.getCatalogue().getId())
                .exporterId(deal.getCatalogue().getExporter().getId())
                .exporterName(exporterName)
                .exporterCountry(exporterCountry)
                .importerId(deal.getRequirement().getImporter().getId())
                .importerName(importerName)
                .importerCountry(importerCountry)
                .productTitle(deal.getCatalogue().getTitle())
                .quantity(deal.getQuantity())
                .supplyPrice(deal.getSupplyPrice())
                .sellPrice(deal.getSellPrice())
                .marginAmount(deal.getMarginAmount())
                .marginPct(deal.getMarginPct())
                .stage(deal.getStage())
                .importerAccepted(deal.isImporterAccepted())
                .deliveryDeadline(deal.getDeliveryDeadline())
                .shippingDocumentUrl(deal.getShippingDocumentUrl())
                .reliabilityScore(BigDecimal.valueOf(100.0)) // Placeholder default
                .auditTrail(Collections.singletonList("Deal initialized in SOURCING stage.")) // Placeholder
                .build();
    }
}
