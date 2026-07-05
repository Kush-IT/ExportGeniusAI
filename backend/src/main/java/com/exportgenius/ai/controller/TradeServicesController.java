package com.exportgenius.ai.controller;

import com.exportgenius.ai.entity.*;
import com.exportgenius.ai.repository.*;
import com.exportgenius.ai.service.StorageService;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping
public class TradeServicesController {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ExporterCatalogueRepository catalogueRepository;
    private final QaReportRepository qaReportRepository;
    private final CertificateRepository certificateRepository;
    private final DealRepository dealRepository;
    private final StorageService storageService;

    public TradeServicesController(TicketRepository ticketRepository,
                                  UserRepository userRepository,
                                  ExporterCatalogueRepository catalogueRepository,
                                  QaReportRepository qaReportRepository,
                                  CertificateRepository certificateRepository,
                                  DealRepository dealRepository,
                                  StorageService storageService) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.catalogueRepository = catalogueRepository;
        this.qaReportRepository = qaReportRepository;
        this.certificateRepository = certificateRepository;
        this.dealRepository = dealRepository;
        this.storageService = storageService;
    }

    // --- SUPPORT TICKETS ---
    
    @PostMapping("/api/tickets")
    @Transactional
    public ResponseEntity<Ticket> createTicket(@RequestBody TicketRequest request, Principal principal) {
        User creator = userRepository.findByEmail(principal.getName()).orElseThrow();
        
        Deal deal = null;
        if (request.getDealId() != null) {
            deal = dealRepository.findById(request.getDealId()).orElse(null);
        }

        Ticket ticket = Ticket.builder()
                .creator(creator)
                .deal(deal)
                .title(request.getTitle())
                .description(request.getDescription())
                .status("OPEN")
                .build();

        Ticket saved = ticketRepository.save(ticket);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/api/tickets")
    public ResponseEntity<List<Ticket>> listMyTickets(Principal principal) {
        User creator = userRepository.findByEmail(principal.getName()).orElseThrow();
        List<Ticket> tickets = ticketRepository.findByCreator(creator);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/api/admin/tickets")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Ticket>> listAllTickets(@RequestParam(value = "status", required = false) String status) {
        List<Ticket> tickets = status != null ? ticketRepository.findByStatus(status) : ticketRepository.findAll();
        return ResponseEntity.ok(tickets);
    }

    @PatchMapping("/api/admin/tickets/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> updateTicket(@PathVariable("id") UUID id, @RequestBody TicketUpdateRequest request, Principal principal) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow();
        User admin = userRepository.findByEmail(principal.getName()).orElseThrow();

        if (request.getStatus() != null) {
            ticket.setStatus(request.getStatus().toUpperCase());
        }
        if (request.isAssignToMe()) {
            ticket.setAssignedAdmin(admin);
        }
        
        Ticket updated = ticketRepository.save(ticket);
        return ResponseEntity.ok(updated);
    }

    // --- QUALITY ASSURANCE ---
    
    @PostMapping("/api/exporter/qa/{catalogueId}")
    @PreAuthorize("hasRole('EXPORTER')")
    @Transactional
    public ResponseEntity<?> submitQaReport(
            @PathVariable("catalogueId") UUID catalogueId,
            @RequestBody QaReportRequest request,
            Principal principal) {

        User exporter = userRepository.findByEmail(principal.getName()).orElseThrow();
        ExporterCatalogue catalogue = catalogueRepository.findByIdAndExporter(catalogueId, exporter)
                .orElseThrow(() -> new IllegalArgumentException("Catalogue item not found or not owned by you"));

        QaReport qaReport = QaReport.builder()
                .catalogue(catalogue)
                .passed(request.isPassed())
                .checklist(request.getChecklist())
                .build();

        QaReport saved = qaReportRepository.save(qaReport);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // --- CERTIFICATIONS ---
    
    @PostMapping(value = "/api/exporter/certificates", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('EXPORTER')")
    @Transactional
    public ResponseEntity<?> uploadCertificate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String certificateType,
            @RequestParam("expiryDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiryDate,
            Principal principal) throws IOException {

        User exporter = userRepository.findByEmail(principal.getName()).orElseThrow();
        String fileUrl = storageService.store(file);

        Certificate certificate = Certificate.builder()
                .exporter(exporter)
                .certificateType(certificateType)
                .fileUrl(fileUrl)
                .expiryDate(expiryDate)
                .build();

        Certificate saved = certificateRepository.save(certificate);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // --- COMPLIANCE ---
    
    @GetMapping("/api/admin/compliance/check")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> checkCompliance(
            @RequestParam("hsCode") String hsCode,
            @RequestParam("country") String country) {
        
        // Simple mock check
        boolean restricted = hsCode != null && hsCode.startsWith("999");
        String status = restricted ? "RESTRICTED" : "ALLOWED";
        String remarks = restricted 
                ? "HS Code prefix '999' is subject to compliance embargoes for " + country 
                : "No restricted prefix matched. Allowed.";

        return ResponseEntity.ok(Map.of(
                "hsCode", hsCode,
                "country", country,
                "status", status,
                "remarks", remarks
        ));
    }

    @Data
    public static class TicketRequest {
        private String title;
        private String description;
        private UUID dealId;
    }

    @Data
    public static class TicketUpdateRequest {
        private String status; // OPEN, IN_REVIEW, RESOLVED, CLOSED
        private boolean assignToMe;
    }

    @Data
    public static class QaReportRequest {
        private String checklist;
        private boolean passed;
    }
}
