package com.exportgenius.ai;

import com.exportgenius.ai.entity.*;
import com.exportgenius.ai.repository.CompanyProfileRepository;
import com.exportgenius.ai.service.PdfTemplateService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class PdfRedactionTest {

    private CompanyProfileRepository companyProfileRepository;
    private PdfTemplateService pdfTemplateService;
    private Deal deal;
    private QaReport qaReport;

    private final String EXPORTER_NAME = "Surat Textiles Pvt Ltd";
    private final String EXPORTER_ADDR = "123 Ring Road, Surat";
    private final String IMPORTER_NAME = "Berlin Imports GmbH";
    private final String IMPORTER_ADDR = "456 Kurfurstendamm, Berlin";

    @BeforeEach
    public void setUp() {
        companyProfileRepository = Mockito.mock(CompanyProfileRepository.class);
        pdfTemplateService = new PdfTemplateService(companyProfileRepository);

        User exporterUser = User.builder()
                .id(UUID.randomUUID())
                .email("surat@textiles.com")
                .fullName(EXPORTER_NAME)
                .build();

        User importerUser = User.builder()
                .id(UUID.randomUUID())
                .email("berlin@imports.de")
                .fullName(IMPORTER_NAME)
                .build();

        Category category = Category.builder().id(1).name("Textiles").build();

        ExporterCatalogue catalogue = ExporterCatalogue.builder()
                .id(UUID.randomUUID())
                .exporter(exporterUser)
                .title("Premium Silk Yarn")
                .category(category)
                .hsCode("500200")
                .supplyPrice(BigDecimal.valueOf(12.50))
                .build();

        Requirement requirement = Requirement.builder()
                .id(UUID.randomUUID())
                .importer(importerUser)
                .productType("Premium Silk Yarn")
                .quantity(500)
                .destinationCountry("Germany")
                .build();

        deal = Deal.builder()
                .id(UUID.randomUUID())
                .catalogue(catalogue)
                .requirement(requirement)
                .quantity(500)
                .supplyPrice(BigDecimal.valueOf(12.50))
                .sellPrice(BigDecimal.valueOf(18.00))
                .marginAmount(BigDecimal.valueOf(2750.00))
                .marginPct(BigDecimal.valueOf(0.3056))
                .stage(DealStage.CONFIRMED)
                .build();

        qaReport = QaReport.builder()
                .id(UUID.randomUUID())
                .catalogue(catalogue)
                .passed(true)
                .checklist("{}")
                .build();

        // Seed mock company profiles
        CompanyProfile exporterProfile = CompanyProfile.builder()
                .user(exporterUser)
                .companyName(EXPORTER_NAME)
                .address(EXPORTER_ADDR)
                .country("India")
                .build();

        CompanyProfile importerProfile = CompanyProfile.builder()
                .user(importerUser)
                .companyName(IMPORTER_NAME)
                .address(IMPORTER_ADDR)
                .country("Germany")
                .build();

        when(companyProfileRepository.findByUser(exporterUser)).thenReturn(Optional.of(exporterProfile));
        when(companyProfileRepository.findByUser(importerUser)).thenReturn(Optional.of(importerProfile));
    }

    private String extractPdfText(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    @Test
    public void testInvoiceContainsImporterButRedactsExporterInfo() throws IOException {
        byte[] pdfBytes = pdfTemplateService.generateInvoice(deal);
        String text = extractPdfText(pdfBytes);

        // Importer PII must be present
        assertTrue(text.contains(IMPORTER_NAME), "Invoice must contain the importer's company name");
        assertTrue(text.contains(IMPORTER_ADDR), "Invoice must contain the importer's address");
        assertTrue(text.contains("18.00"), "Invoice must show the sell price ($18.00)");

        // Exporter PII and supply price MUST be redacted/absent
        assertFalse(text.contains(EXPORTER_NAME), "Invoice must NOT contain the exporter's name");
        assertFalse(text.contains(EXPORTER_ADDR), "Invoice must NOT contain the exporter's address");
        assertFalse(text.contains("12.50"), "Invoice must NOT contain the supply price ($12.50)");
    }

    @Test
    public void testPurchaseOrderContainsExporterButRedactsImporterInfo() throws IOException {
        byte[] pdfBytes = pdfTemplateService.generatePurchaseOrder(deal);
        String text = extractPdfText(pdfBytes);

        // Exporter PII must be present
        assertTrue(text.contains(EXPORTER_NAME), "Purchase Order must contain the exporter's company name");
        assertTrue(text.contains(EXPORTER_ADDR), "Purchase Order must contain the exporter's address");
        assertTrue(text.contains("12.50"), "Purchase Order must show the supply price ($12.50)");

        // Importer PII and sell price MUST be redacted/absent
        assertFalse(text.contains(IMPORTER_NAME), "Purchase Order must NOT contain the importer's name");
        assertFalse(text.contains(IMPORTER_ADDR), "Purchase Order must NOT contain the importer's address");
        assertFalse(text.contains("18.00"), "Purchase Order must NOT contain the sell price ($18.00)");
    }

    @Test
    public void testQualityCertificateRedactsExporterInfo() throws IOException {
        byte[] pdfBytes = pdfTemplateService.generateQualityCertificate(deal, qaReport);
        String text = extractPdfText(pdfBytes);

        // Exporter details MUST be absent
        assertFalse(text.contains(EXPORTER_NAME), "Quality Certificate must NOT contain the exporter's name");
    }

    @Test
    public void testTradeAgreementContainsImporterButRedactsExporterInfo() throws IOException {
        byte[] pdfBytes = pdfTemplateService.generateTradeAgreement(deal);
        String text = extractPdfText(pdfBytes);

        // Importer and platform details present
        assertTrue(text.contains(IMPORTER_NAME), "Trade Agreement must contain the importer's name");
        assertTrue(text.contains("ExportGenius AI"), "Trade Agreement must contain the broker name");

        // Exporter details MUST be absent
        assertFalse(text.contains(EXPORTER_NAME), "Trade Agreement must NOT contain the exporter's name");
    }
}
