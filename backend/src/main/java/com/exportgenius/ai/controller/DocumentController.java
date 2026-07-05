package com.exportgenius.ai.controller;

import com.exportgenius.ai.entity.*;
import com.exportgenius.ai.repository.*;
import com.exportgenius.ai.service.PdfTemplateService;
import com.exportgenius.ai.service.StorageService;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping
public class DocumentController {

    private final DealRepository dealRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final QaReportRepository qaReportRepository;
    private final PdfTemplateService pdfTemplateService;
    private final StorageService storageService;

    public DocumentController(DealRepository dealRepository,
                              UserRepository userRepository,
                              DocumentRepository documentRepository,
                              QaReportRepository qaReportRepository,
                              PdfTemplateService pdfTemplateService,
                              StorageService storageService) {
        this.dealRepository = dealRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.qaReportRepository = qaReportRepository;
        this.pdfTemplateService = pdfTemplateService;
        this.storageService = storageService;
    }

    @PostMapping("/api/admin/documents/generate")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> generateDocument(@RequestBody GenerateDocumentRequest request, Principal principal) throws IOException {
        Deal deal = dealRepository.findById(request.getDealId())
                .orElseThrow(() -> new IllegalArgumentException("Deal not found"));

        User admin = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Admin profile not found"));

        String docType = request.getDocumentType().toUpperCase();
        byte[] pdfBytes;

        switch (docType) {
            case "INVOICE" -> pdfBytes = pdfTemplateService.generateInvoice(deal);
            case "PURCHASE_ORDER" -> pdfBytes = pdfTemplateService.generatePurchaseOrder(deal);
            case "QUALITY_CERTIFICATE" -> {
                QaReport report = qaReportRepository
                        .findFirstByCatalogueAndPassedTrueOrderByCreatedAtDesc(deal.getCatalogue())
                        .orElse(null); // QualityCert can render even if report is missing during test/fallbacks
                pdfBytes = pdfTemplateService.generateQualityCertificate(deal, report);
            }
            case "TRADE_AGREEMENT" -> pdfBytes = pdfTemplateService.generateTradeAgreement(deal);
            default -> {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid document type: " + docType));
            }
        }

        String filename = docType.toLowerCase() + "_" + deal.getId().toString() + "_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
        String fileUrl = storageService.storeBytes(pdfBytes, filename);

        Document document = Document.builder()
                .deal(deal)
                .documentType(docType)
                .fileUrl(fileUrl)
                .uploadedBy(admin)
                .build();

        Document savedDoc = documentRepository.save(document);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDoc);
    }

    @GetMapping("/api/exporter/documents/{dealId}")
    @PreAuthorize("hasRole('EXPORTER')")
    public ResponseEntity<?> getExporterDocuments(@PathVariable("dealId") UUID dealId, Principal principal) {
        User exporter = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Exporter profile not found"));

        Deal deal = dealRepository.findByIdAndCatalogueExporter(dealId, exporter)
                .orElseThrow(() -> new IllegalArgumentException("Deal not found or does not belong to you"));

        // Exporter can only see PURCHASE_ORDER
        List<Document> docs = documentRepository.findByDealAndDocumentTypeIn(deal, List.of("PURCHASE_ORDER"));
        return ResponseEntity.ok(docs);
    }

    @GetMapping("/api/importer/documents/{dealId}")
    @PreAuthorize("hasRole('IMPORTER')")
    public ResponseEntity<?> getImporterDocuments(@PathVariable("dealId") UUID dealId, Principal principal) {
        User importer = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Importer profile not found"));

        Deal deal = dealRepository.findByIdAndRequirementImporter(dealId, importer)
                .orElseThrow(() -> new IllegalArgumentException("Deal not found or does not belong to you"));

        // Importer can see INVOICE, QUALITY_CERTIFICATE, TRADE_AGREEMENT
        List<Document> docs = documentRepository.findByDealAndDocumentTypeIn(
                deal, 
                List.of("INVOICE", "QUALITY_CERTIFICATE", "TRADE_AGREEMENT")
        );
        return ResponseEntity.ok(docs);
    }

    @Data
    public static class GenerateDocumentRequest {
        private UUID dealId;
        private String documentType; // INVOICE, PURCHASE_ORDER, QUALITY_CERTIFICATE, TRADE_AGREEMENT
    }
}
