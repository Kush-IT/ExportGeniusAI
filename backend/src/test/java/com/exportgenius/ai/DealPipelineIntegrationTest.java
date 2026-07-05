package com.exportgenius.ai;

import com.exportgenius.ai.entity.*;
import com.exportgenius.ai.exception.IllegalStateTransitionException;
import com.exportgenius.ai.repository.*;
import com.exportgenius.ai.service.DealStageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DealPipelineIntegrationTest {

    @Autowired
    private DealRepository dealRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExporterCatalogueRepository catalogueRepository;

    @Autowired
    private RequirementRepository requirementRepository;

    @Autowired
    private QaReportRepository qaReportRepository;

    @Autowired
    private PaymentInRepository paymentInRepository;

    @Autowired
    private PaymentOutRepository paymentOutRepository;

    @Autowired
    private MarginLedgerRepository marginLedgerRepository;

    @Autowired
    private DealStageService dealStageService;

    private User exporter;
    private User importer;
    private User admin;
    private Category category;
    private ExporterCatalogue catalogueItem;
    private Requirement requirement;

    @BeforeEach
    public void setUp() {
        // Seed Roles
        Role exporterRole = roleRepository.findByName(Role.EXPORTER)
                .orElseGet(() -> roleRepository.save(Role.builder().id(1).name(Role.EXPORTER).build()));
        Role adminRole = roleRepository.findByName(Role.ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().id(2).name(Role.ADMIN).build()));
        Role importerRole = roleRepository.findByName(Role.IMPORTER)
                .orElseGet(() -> roleRepository.save(Role.builder().id(3).name(Role.IMPORTER).build()));

        // Create Users
        exporter = userRepository.save(User.builder()
                .email("exporter_pipeline@test.com")
                .password("password")
                .fullName("Pipeline Exporter")
                .isActive(true)
                .roles(Set.of(exporterRole))
                .build());

        importer = userRepository.save(User.builder()
                .email("importer_pipeline@test.com")
                .password("password")
                .fullName("Pipeline Importer")
                .isActive(true)
                .roles(Set.of(importerRole))
                .build());

        admin = userRepository.save(User.builder()
                .email("admin_pipeline@test.com")
                .password("password")
                .fullName("Pipeline Admin")
                .isActive(true)
                .roles(Set.of(adminRole))
                .build());

        // Create Category
        category = categoryRepository.save(Category.builder().id(10).name("Spices Pipeline").build());

        // Create Catalogue Item (Supply Price: $10.00)
        catalogueItem = catalogueRepository.save(ExporterCatalogue.builder()
                .exporter(exporter)
                .title("Organic Cumin")
                .category(category)
                .hsCode("090931")
                .supplyPrice(BigDecimal.valueOf(10.00))
                .moq(100)
                .leadTimeDays(15)
                .isActive(true)
                .build());

        // Create Requirement (Quantity: 200, Country: UAE)
        requirement = requirementRepository.save(Requirement.builder()
                .importer(importer)
                .productType("Organic Cumin")
                .quantity(200)
                .destinationCountry("UAE")
                .status("OPEN")
                .build());
    }

    @Test
    public void testFullHappyPathStateTransitions() {
        // 1. Create a deal initially in SOURCING
        Deal deal = Deal.builder()
                .requirement(requirement)
                .catalogue(catalogueItem)
                .quantity(requirement.getQuantity())
                .supplyPrice(catalogueItem.getSupplyPrice())
                .sellPrice(catalogueItem.getSupplyPrice())
                .marginAmount(BigDecimal.ZERO)
                .marginPct(BigDecimal.ZERO)
                .stage(DealStage.SOURCING)
                .importerAccepted(false)
                .build();
        deal = dealRepository.save(deal);
        UUID dealId = deal.getId();

        assertEquals(DealStage.SOURCING, deal.getStage());

        // 2. Admin quotes a sell price of $15.00 (margin: $5.00, marginPct: 33.3%)
        BigDecimal sellPrice = BigDecimal.valueOf(15.00);
        BigDecimal supplyPrice = deal.getSupplyPrice();
        BigDecimal quantityBD = BigDecimal.valueOf(deal.getQuantity());
        BigDecimal totalMarginAmount = sellPrice.subtract(supplyPrice).multiply(quantityBD);
        BigDecimal marginPct = sellPrice.subtract(supplyPrice).divide(sellPrice, 4, java.math.RoundingMode.HALF_UP);

        deal.setSellPrice(sellPrice);
        deal.setMarginAmount(totalMarginAmount);
        deal.setMarginPct(marginPct);
        dealRepository.save(deal);

        Deal quotedDeal = dealStageService.advanceStage(dealId, DealStage.QUOTED, admin.getEmail());
        assertEquals(DealStage.QUOTED, quotedDeal.getStage());

        // 3. Importer accepts quote
        quotedDeal.setImporterAccepted(true);
        dealRepository.save(quotedDeal);

        // 4. Create a passing QA Report
        qaReportRepository.save(QaReport.builder()
                .catalogue(catalogueItem)
                .passed(true)
                .checklist("{}")
                .build());

        // 5. Admin confirms deal
        Deal confirmedDeal = dealStageService.advanceStage(dealId, DealStage.CONFIRMED, admin.getEmail());
        assertEquals(DealStage.CONFIRMED, confirmedDeal.getStage());

        // Assert Side Effects: margin ledger and payments created
        assertTrue(marginLedgerRepository.findByDeal(confirmedDeal).isPresent());
        
        PaymentIn paymentIn = paymentInRepository.findFirstByDealOrderByCreatedAtDesc(confirmedDeal).orElse(null);
        assertNotNull(paymentIn);
        assertEquals("PENDING", paymentIn.getStatus());
        assertEquals(BigDecimal.valueOf(3000.00).setScale(2), paymentIn.getAmount().setScale(2)); // 200 quantity * $15.00

        PaymentOut paymentOut = paymentOutRepository.findFirstByDealOrderByCreatedAtDesc(confirmedDeal).orElse(null);
        assertNotNull(paymentOut);
        assertEquals("PENDING", paymentOut.getStatus());
        assertEquals(BigDecimal.valueOf(2000.00).setScale(2), paymentOut.getAmount().setScale(2)); // 200 quantity * $10.00

        // 6. Simulate receiving payment from importer
        paymentIn.setStatus("RECEIVED");
        paymentInRepository.save(paymentIn);

        // Upload shipping doc & Dispatch
        confirmedDeal.setShippingDocumentUrl("http://s3.exportgenius.com/docs/bol_cumin.pdf");
        dealRepository.save(confirmedDeal);

        Deal dispatchedDeal = dealStageService.advanceStage(dealId, DealStage.DISPATCHED, exporter.getEmail());
        assertEquals(DealStage.DISPATCHED, dispatchedDeal.getStage());

        // 7. Simulate admin settling payout to exporter
        paymentOut.setStatus("SETTLED");
        paymentOutRepository.save(paymentOut);

        // Advance to PAID
        Deal paidDeal = dealStageService.advanceStage(dealId, DealStage.PAID, admin.getEmail());
        assertEquals(DealStage.PAID, paidDeal.getStage());

        // Assert final side effects
        MarginLedger ledger = marginLedgerRepository.findByDeal(paidDeal).orElse(null);
        assertNotNull(ledger);
        assertNotNull(ledger.getCapturedAt());
        assertEquals("CLOSED", paidDeal.getRequirement().getStatus());
    }

    @Test
    public void testStageSkippingIsBlocked() {
        Deal deal = Deal.builder()
                .requirement(requirement)
                .catalogue(catalogueItem)
                .quantity(requirement.getQuantity())
                .supplyPrice(catalogueItem.getSupplyPrice())
                .sellPrice(catalogueItem.getSupplyPrice())
                .marginAmount(BigDecimal.ZERO)
                .marginPct(BigDecimal.ZERO)
                .stage(DealStage.SOURCING)
                .importerAccepted(false)
                .build();
        deal = dealRepository.save(deal);
        UUID dealId = deal.getId();

        // Try transitioning directly from SOURCING to CONFIRMED
        assertThrows(IllegalStateTransitionException.class, () -> {
            dealStageService.advanceStage(dealId, DealStage.CONFIRMED, admin.getEmail());
        });
    }
}
