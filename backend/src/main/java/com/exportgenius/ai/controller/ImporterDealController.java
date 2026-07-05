package com.exportgenius.ai.controller;

import com.exportgenius.ai.dto.importer.ImporterDealDTO;
import com.exportgenius.ai.entity.Deal;
import com.exportgenius.ai.entity.DealStage;
import com.exportgenius.ai.entity.User;
import com.exportgenius.ai.mapper.DealMapper;
import com.exportgenius.ai.repository.DealRepository;
import com.exportgenius.ai.repository.UserRepository;
import com.exportgenius.ai.service.DealStageService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/importer/deals")
@PreAuthorize("hasRole('IMPORTER')")
public class ImporterDealController {

    private final DealRepository dealRepository;
    private final UserRepository userRepository;
    private final DealStageService dealStageService;
    private final DealMapper dealMapper;

    public ImporterDealController(DealRepository dealRepository,
                                  UserRepository userRepository,
                                  DealStageService dealStageService,
                                  DealMapper dealMapper) {
        this.dealRepository = dealRepository;
        this.userRepository = userRepository;
        this.dealStageService = dealStageService;
        this.dealMapper = dealMapper;
    }

    @GetMapping
    public ResponseEntity<List<ImporterDealDTO>> listDeals(Principal principal) {
        User importer = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Importer profile not found"));

        List<Deal> deals = dealRepository.findByRequirementImporter(importer);
        List<ImporterDealDTO> dtos = deals.stream()
                .map(dealMapper::toImporterDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PatchMapping("/{id}/respond")
    @Transactional
    public ResponseEntity<?> respondToQuote(@PathVariable("id") UUID id, @RequestBody QuoteResponseRequest request, Principal principal) {
        User importer = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Importer profile not found"));

        Deal deal = dealRepository.findByIdAndRequirementImporter(id, importer)
                .orElseThrow(() -> new IllegalArgumentException("Deal not found or does not belong to you"));

        String action = request.getAction().toUpperCase();

        if (action.equals("ACCEPT")) {
            deal.setImporterAccepted(true);
            dealRepository.save(deal);
            // Accept does not automatically advance stage to CONFIRMED. Admin does that once QA checklist passes.
        } else if (action.equals("COUNTER") || action.equals("REJECT")) {
            deal.setImporterAccepted(false);
            dealRepository.save(deal);

            // If deal was QUOTED, move it back to NEGOTIATING
            if (deal.getStage() == DealStage.QUOTED) {
                dealStageService.advanceStage(id, DealStage.NEGOTIATING, principal.getName());
            }

            System.out.println("Importer responded with: " + action 
                    + (request.getCounterPrice() != null ? " (Counter Offer: " + request.getCounterPrice() + ")" : ""));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Action must be ACCEPT, COUNTER, or REJECT."));
        }

        Deal updatedDeal = dealRepository.findById(id).orElseThrow();
        return ResponseEntity.ok(dealMapper.toImporterDTO(updatedDeal));
    }

    @Data
    public static class QuoteResponseRequest {
        private String action; // ACCEPT, COUNTER, REJECT
        private BigDecimal counterPrice;
    }
}
