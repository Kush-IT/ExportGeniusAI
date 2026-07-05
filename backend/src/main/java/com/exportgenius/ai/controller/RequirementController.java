package com.exportgenius.ai.controller;

import com.exportgenius.ai.dto.RequirementRequest;
import com.exportgenius.ai.entity.Requirement;
import com.exportgenius.ai.entity.User;
import com.exportgenius.ai.repository.RequirementRepository;
import com.exportgenius.ai.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/importer/requirements")
@PreAuthorize("hasRole('IMPORTER')")
public class RequirementController {

    private final RequirementRepository requirementRepository;
    private final UserRepository userRepository;

    public RequirementController(RequirementRepository requirementRepository, UserRepository userRepository) {
        this.requirementRepository = requirementRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Requirement> createRequirement(@Valid @RequestBody RequirementRequest request, Principal principal) {
        User importer = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Importer profile not found"));

        Requirement requirement = Requirement.builder()
                .importer(importer)
                .productType(request.getProductType())
                .quantity(request.getQuantity())
                .qualityStandard(request.getQualityStandard())
                .targetPrice(request.getTargetPrice())
                .destinationCountry(request.getDestinationCountry())
                .timeline(request.getTimeline())
                .status("OPEN")
                .build();

        Requirement savedRequirement = requirementRepository.save(requirement);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedRequirement);
    }

    @GetMapping
    public ResponseEntity<List<Requirement>> listRequirements(Principal principal) {
        User importer = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Importer profile not found"));

        List<Requirement> requirements = requirementRepository.findByImporter(importer);
        return ResponseEntity.ok(requirements);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> editRequirement(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RequirementRequest request,
            Principal principal) {

        User importer = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Importer profile not found"));

        Requirement existing = requirementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Requirement not found"));

        // Enforce ownership: 403 if not the owner
        if (!existing.getImporter().getId().equals(importer.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not own this requirement."));
        }

        // Editable only while status == OPEN
        if (!existing.getStatus().equals("OPEN")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Requirements can only be edited while status is 'OPEN'"));
        }

        existing.setProductType(request.getProductType());
        existing.setQuantity(request.getQuantity());
        existing.setQualityStandard(request.getQualityStandard());
        existing.setTargetPrice(request.getTargetPrice());
        existing.setDestinationCountry(request.getDestinationCountry());
        existing.setTimeline(request.getTimeline());

        Requirement updated = requirementRepository.save(existing);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> withdrawRequirement(@PathVariable("id") UUID id, Principal principal) {
        User importer = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Importer profile not found"));

        Requirement existing = requirementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Requirement not found"));

        // Enforce ownership
        if (!existing.getImporter().getId().equals(importer.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not own this requirement."));
        }

        // Withdrawable only while status == OPEN
        if (!existing.getStatus().equals("OPEN")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Requirements can only be withdrawn while status is 'OPEN'"));
        }

        existing.setStatus("CLOSED"); // Withdraw the requirement by closing it
        requirementRepository.save(existing);

        return ResponseEntity.ok(Map.of("message", "Requirement withdrawn successfully (status set to CLOSED)."));
    }
}
