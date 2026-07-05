package com.exportgenius.ai.controller;

import com.exportgenius.ai.dto.CatalogueRequest;
import com.exportgenius.ai.entity.Category;
import com.exportgenius.ai.entity.ExporterCatalogue;
import com.exportgenius.ai.entity.ProductImage;
import com.exportgenius.ai.entity.User;
import com.exportgenius.ai.repository.CategoryRepository;
import com.exportgenius.ai.repository.ExporterCatalogueRepository;
import com.exportgenius.ai.repository.ProductImageRepository;
import com.exportgenius.ai.repository.UserRepository;
import com.exportgenius.ai.service.StorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/exporter/catalogue")
@PreAuthorize("hasRole('EXPORTER')")
public class ExporterCatalogueController {

    private final ExporterCatalogueRepository catalogueRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ProductImageRepository productImageRepository;
    private final StorageService storageService;

    public ExporterCatalogueController(ExporterCatalogueRepository catalogueRepository,
                                       CategoryRepository categoryRepository,
                                       UserRepository userRepository,
                                       ProductImageRepository productImageRepository,
                                       StorageService storageService) {
        this.catalogueRepository = catalogueRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.productImageRepository = productImageRepository;
        this.storageService = storageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> createEntry(
            @Valid @RequestPart("catalogue") CatalogueRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            Principal principal) throws IOException {

        User exporter = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Exporter profile not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid category ID"));

        ExporterCatalogue catalogueEntry = ExporterCatalogue.builder()
                .exporter(exporter)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(category)
                .hsCode(request.getHsCode())
                .supplyPrice(request.getSupplyPrice())
                .currency(request.getCurrency())
                .moq(request.getMoq())
                .leadTimeDays(request.getLeadTimeDays())
                .productionCapacity(request.getProductionCapacity())
                .isActive(true)
                .build();

        ExporterCatalogue savedEntry = catalogueRepository.save(catalogueEntry);

        if (images != null && !images.isEmpty()) {
            if (images.size() > 5) {
                return ResponseEntity.badRequest().body(Map.of("error", "You can upload a maximum of 5 images."));
            }
            for (MultipartFile image : images) {
                if (image != null && !image.isEmpty()) {
                    String imageUrl = storageService.store(image);
                    ProductImage productImage = ProductImage.builder()
                            .product(savedEntry)
                            .imageUrl(imageUrl)
                            .build();
                    productImageRepository.save(productImage);
                }
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(savedEntry);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> editEntry(
            @PathVariable("id") UUID id,
            @Valid @RequestPart("catalogue") CatalogueRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            Principal principal) throws IOException {

        User exporter = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Exporter profile not found"));

        ExporterCatalogue existingEntry = catalogueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Catalogue entry not found"));

        // Enforce ownership: 403 if authenticated user is not the owner
        if (!existingEntry.getExporter().getId().equals(exporter.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not own this catalogue entry."));
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid category ID"));

        existingEntry.setTitle(request.getTitle());
        existingEntry.setDescription(request.getDescription());
        existingEntry.setCategory(category);
        existingEntry.setHsCode(request.getHsCode());
        existingEntry.setSupplyPrice(request.getSupplyPrice());
        existingEntry.setCurrency(request.getCurrency());
        existingEntry.setMoq(request.getMoq());
        existingEntry.setLeadTimeDays(request.getLeadTimeDays());
        existingEntry.setProductionCapacity(request.getProductionCapacity());

        ExporterCatalogue updatedEntry = catalogueRepository.save(existingEntry);

        if (images != null && !images.isEmpty()) {
            List<ProductImage> currentImages = productImageRepository.findByProduct(updatedEntry);
            if (currentImages.size() + images.size() > 5) {
                return ResponseEntity.badRequest().body(Map.of("error", "Total images for this entry cannot exceed 5."));
            }
            for (MultipartFile image : images) {
                if (image != null && !image.isEmpty()) {
                    String imageUrl = storageService.store(image);
                    ProductImage productImage = ProductImage.builder()
                            .product(updatedEntry)
                            .imageUrl(imageUrl)
                            .build();
                    productImageRepository.save(productImage);
                }
            }
        }

        return ResponseEntity.ok(updatedEntry);
    }

    @GetMapping
    public ResponseEntity<List<ExporterCatalogue>> listEntries(Principal principal) {
        User exporter = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Exporter profile not found"));

        List<ExporterCatalogue> entries = catalogueRepository.findByExporterAndIsActiveTrue(exporter);
        return ResponseEntity.ok(entries);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> deleteEntry(@PathVariable("id") UUID id, Principal principal) {
        User exporter = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Exporter profile not found"));

        ExporterCatalogue entry = catalogueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Catalogue entry not found"));

        // Enforce ownership
        if (!entry.getExporter().getId().equals(exporter.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not own this catalogue entry."));
        }

        entry.setActive(false); // Soft delete
        catalogueRepository.save(entry);

        return ResponseEntity.ok(Map.of("message", "Catalogue entry soft-deleted successfully."));
    }
}
