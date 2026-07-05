package com.exportgenius.ai.controller;

import com.exportgenius.ai.dto.admin.AdminDealDTO;
import com.exportgenius.ai.entity.*;
import com.exportgenius.ai.mapper.DealMapper;
import com.exportgenius.ai.repository.DealRepository;
import com.exportgenius.ai.repository.ExporterCatalogueRepository;
import com.exportgenius.ai.repository.RequirementRepository;
import com.exportgenius.ai.service.DealStageService;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/deals")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDealController {

    private final DealRepository dealRepository;
    private final RequirementRepository requirementRepository;
    private final ExporterCatalogueRepository catalogueRepository;
    private final DealStageService dealStageService;
    private final DealMapper dealMapper;

    public AdminDealController(DealRepository dealRepository,
                               RequirementRepository requirementRepository,
                               ExporterCatalogueRepository catalogueRepository,
                               DealStageService dealStageService,
                               DealMapper dealMapper) {
        this.dealRepository = dealRepository;
        this.requirementRepository = requirementRepository;
        this.catalogueRepository = catalogueRepository;
        this.dealStageService = dealStageService;
        this.dealMapper = dealMapper;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createDeal(@RequestBody CreateDealRequest request) {
        Requirement requirement = requirementRepository.findById(request.getRequirementId())
                .orElseThrow(() -> new IllegalArgumentException("Requirement not found"));

        ExporterCatalogue catalogue = catalogueRepository.findById(request.getCatalogueId())
                .orElseThrow(() -> new IllegalArgumentException("Catalogue item not found"));

        BigDecimal supplyPrice = catalogue.getSupplyPrice();
        Integer quantity = requirement.getQuantity();

        // Create deal
        Deal deal = Deal.builder()
                .requirement(requirement)
                .catalogue(catalogue)
                .quantity(quantity)
                .supplyPrice(supplyPrice)
                .sellPrice(supplyPrice) // Initially equal to supplyPrice before admin quotes
                .marginAmount(BigDecimal.ZERO)
                .marginPct(BigDecimal.ZERO)
                .stage(DealStage.SOURCING)
                .importerAccepted(false)
                .build();

        Deal savedDeal = dealRepository.save(deal);
        
        // Update requirement status
        requirement.setStatus("MATCHED");
        requirementRepository.save(requirement);

        return ResponseEntity.status(HttpStatus.CREATED).body(dealMapper.toAdminDTO(savedDeal));
    }

    @PostMapping("/{id}/quote")
    @Transactional
    public ResponseEntity<?> submitQuote(@PathVariable("id") UUID id, @RequestBody QuoteRequest request, Principal principal) {
        Deal deal = dealRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Deal not found"));

        BigDecimal sellPrice = request.getSellPrice();
        BigDecimal supplyPrice = deal.getSupplyPrice();

        if (sellPrice.compareTo(supplyPrice) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sell price must exceed supply price."));
        }

        BigDecimal quantityBD = BigDecimal.valueOf(deal.getQuantity());
        BigDecimal marginPerUnit = sellPrice.subtract(supplyPrice);
        BigDecimal totalMarginAmount = marginPerUnit.multiply(quantityBD);
        BigDecimal marginPct = marginPerUnit.divide(sellPrice, 4, RoundingMode.HALF_UP);

        deal.setSellPrice(sellPrice);
        deal.setMarginAmount(totalMarginAmount);
        deal.setMarginPct(marginPct);

        // Advance to QUOTED
        Deal updatedDeal = dealStageService.advanceStage(id, DealStage.QUOTED, principal.getName());

        return ResponseEntity.ok(dealMapper.toAdminDTO(updatedDeal));
    }

    @PatchMapping("/{id}/stage")
    public ResponseEntity<?> advanceStage(@PathVariable("id") UUID id, @RequestBody Map<String, String> body, Principal principal) {
        String targetStageStr = body.get("targetStage");
        if (targetStageStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "targetStage is required"));
        }

        DealStage targetStage = DealStage.valueOf(targetStageStr.toUpperCase());
        Deal updatedDeal = dealStageService.advanceStage(id, targetStage, principal.getName());
        return ResponseEntity.ok(dealMapper.toAdminDTO(updatedDeal));
    }

    @GetMapping
    public ResponseEntity<List<AdminDealDTO>> listDeals(@RequestParam(value = "stage", required = false) String stageStr) {
        List<Deal> deals;
        if (stageStr != null && !stageStr.isEmpty()) {
            DealStage stage = DealStage.valueOf(stageStr.toUpperCase());
            deals = dealRepository.findByStage(stage);
        } else {
            deals = dealRepository.findAll();
        }

        List<AdminDealDTO> dtos = deals.stream()
                .map(dealMapper::toAdminDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @Data
    public static class CreateDealRequest {
        private UUID requirementId;
        private UUID catalogueId;
    }

    @Data
    public static class QuoteRequest {
        private BigDecimal sellPrice;
    }
}
