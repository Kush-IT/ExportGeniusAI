package com.exportgenius.ai.controller;

import com.exportgenius.ai.dto.exporter.ExporterDealDTO;
import com.exportgenius.ai.entity.Deal;
import com.exportgenius.ai.entity.DealStage;
import com.exportgenius.ai.entity.User;
import com.exportgenius.ai.mapper.DealMapper;
import com.exportgenius.ai.repository.DealRepository;
import com.exportgenius.ai.repository.UserRepository;
import com.exportgenius.ai.service.DealStageService;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/exporter/deals")
@PreAuthorize("hasRole('EXPORTER')")
public class ExporterDealController {

    private final DealRepository dealRepository;
    private final UserRepository userRepository;
    private final DealStageService dealStageService;
    private final DealMapper dealMapper;

    public ExporterDealController(DealRepository dealRepository,
                                  UserRepository userRepository,
                                  DealStageService dealStageService,
                                  DealMapper dealMapper) {
        this.dealRepository = dealRepository;
        this.userRepository = userRepository;
        this.dealStageService = dealStageService;
        this.dealMapper = dealMapper;
    }

    @GetMapping
    public ResponseEntity<List<ExporterDealDTO>> listDeals(Principal principal) {
        User exporter = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Exporter profile not found"));

        List<Deal> deals = dealRepository.findByCatalogueExporter(exporter);
        List<ExporterDealDTO> dtos = deals.stream()
                .map(dealMapper::toExporterDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PatchMapping("/{id}/dispatch")
    @Transactional
    public ResponseEntity<?> dispatchDeal(@PathVariable("id") UUID id, @RequestBody DispatchRequest request, Principal principal) {
        User exporter = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Exporter profile not found"));

        Deal deal = dealRepository.findByIdAndCatalogueExporter(id, exporter)
                .orElseThrow(() -> new IllegalArgumentException("Deal not found or does not belong to you"));

        if (request.getShippingDocumentUrl() == null || request.getShippingDocumentUrl().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Shipping document URL is required to dispatch."));
        }

        deal.setShippingDocumentUrl(request.getShippingDocumentUrl());
        dealRepository.save(deal);

        // Transition from CONFIRMED to DISPATCHED
        Deal updatedDeal = dealStageService.advanceStage(id, DealStage.DISPATCHED, principal.getName());

        return ResponseEntity.ok(dealMapper.toExporterDTO(updatedDeal));
    }

    @Data
    public static class DispatchRequest {
        private String shippingDocumentUrl;
    }
}
