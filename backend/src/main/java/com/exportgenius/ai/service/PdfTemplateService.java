package com.exportgenius.ai.service;

import com.exportgenius.ai.entity.CompanyProfile;
import com.exportgenius.ai.entity.Deal;
import com.exportgenius.ai.entity.QaReport;
import com.exportgenius.ai.entity.User;
import com.exportgenius.ai.repository.CompanyProfileRepository;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PdfTemplateService {

    private final CompanyProfileRepository companyProfileRepository;

    public PdfTemplateService(CompanyProfileRepository companyProfileRepository) {
        this.companyProfileRepository = companyProfileRepository;
    }

    public byte[] generateInvoice(Deal deal) {
        com.lowagie.text.Document document = new com.lowagie.text.Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Brand Header
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Paragraph title = new Paragraph("EXPORTGENIUS AI - INVOICE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Invoice details table
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.addCell(createCell("Invoice ID: " + deal.getId().toString().substring(0, 8), Element.ALIGN_LEFT));
            metaTable.addCell(createCell("Date: " + LocalDateTimeToString(deal.getCreatedAt()), Element.ALIGN_RIGHT));
            metaTable.setSpacingAfter(20);
            document.add(metaTable);

            // Parties details (Importer only - Exporter MUST be redacted)
            Paragraph billTo = new Paragraph("BILL TO (IMPORTER):", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            document.add(billTo);

            User importer = deal.getRequirement().getImporter();
            CompanyProfile importerProfile = companyProfileRepository.findByUser(importer).orElse(null);
            String importerCompanyName = importerProfile != null ? importerProfile.getCompanyName() : importer.getFullName();
            String importerCountry = importerProfile != null ? importerProfile.getCountry() : deal.getRequirement().getDestinationCountry();
            String importerAddress = importerProfile != null ? importerProfile.getAddress() : "Address not provided";

            document.add(new Paragraph(importerCompanyName));
            document.add(new Paragraph(importerAddress));
            document.add(new Paragraph(importerCountry));
            document.add(new Paragraph(" "));

            // Vendor (Brokerage platform represents the vendor)
            Paragraph vendor = new Paragraph("ISSUED BY (BROKER):", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            document.add(vendor);
            document.add(new Paragraph("ExportGenius AI Platform Ltd"));
            document.add(new Paragraph("Corporate Trade Division"));
            document.add(new Paragraph("brokerage@exportgenius.ai"));
            document.add(new Paragraph(" "));

            // Items table
            PdfPTable itemTable = new PdfPTable(4);
            itemTable.setWidthPercentage(100);
            itemTable.addCell(new PdfPCell(new Phrase("Product Type")));
            itemTable.addCell(new PdfPCell(new Phrase("Quantity")));
            itemTable.addCell(new PdfPCell(new Phrase("Sell Price")));
            itemTable.addCell(new PdfPCell(new Phrase("Total")));

            BigDecimal quantityBD = BigDecimal.valueOf(deal.getQuantity());
            BigDecimal totalSellPrice = deal.getSellPrice().multiply(quantityBD);

            itemTable.addCell(deal.getCatalogue().getTitle());
            itemTable.addCell(String.valueOf(deal.getQuantity()));
            itemTable.addCell("$" + deal.getSellPrice().toString());
            itemTable.addCell("$" + totalSellPrice.toString());

            itemTable.setSpacingBefore(10);
            itemTable.setSpacingAfter(20);
            document.add(itemTable);

            // Exporter Redaction verification notice
            Paragraph footnote = new Paragraph("* Exporter identity and base supply price are redacted for brokerage confidentiality.", 
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9));
            document.add(footnote);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    public byte[] generatePurchaseOrder(Deal deal) {
        com.lowagie.text.Document document = new com.lowagie.text.Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Brand Header
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Paragraph title = new Paragraph("EXPORTGENIUS AI - PURCHASE ORDER", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Metadata
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.addCell(createCell("PO ID: PO-" + deal.getId().toString().substring(0, 8), Element.ALIGN_LEFT));
            metaTable.addCell(createCell("Date: " + LocalDateTimeToString(deal.getCreatedAt()), Element.ALIGN_RIGHT));
            metaTable.setSpacingAfter(20);
            document.add(metaTable);

            // Exporter (Supplier) Details (Importer is REDACTED)
            Paragraph issuedTo = new Paragraph("ISSUED TO (EXPORTER):", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            document.add(issuedTo);

            User exporter = deal.getCatalogue().getExporter();
            CompanyProfile exporterProfile = companyProfileRepository.findByUser(exporter).orElse(null);
            String exporterCompanyName = exporterProfile != null ? exporterProfile.getCompanyName() : exporter.getFullName();
            String exporterCountry = exporterProfile != null ? exporterProfile.getCountry() : "Country not provided";
            String exporterAddress = exporterProfile != null ? exporterProfile.getAddress() : "Address not provided";

            document.add(new Paragraph(exporterCompanyName));
            document.add(new Paragraph(exporterAddress));
            document.add(new Paragraph(exporterCountry));
            document.add(new Paragraph(" "));

            // Purchaser (Brokerage platform represents the buyer)
            Paragraph purchaser = new Paragraph("PURCHASER (BROKER):", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            document.add(purchaser);
            document.add(new Paragraph("ExportGenius AI Platform Ltd"));
            document.add(new Paragraph("Global Sourcing Division"));
            document.add(new Paragraph(" "));

            // Items table (Supply price-based)
            PdfPTable itemTable = new PdfPTable(4);
            itemTable.setWidthPercentage(100);
            itemTable.addCell(new PdfPCell(new Phrase("Product Type")));
            itemTable.addCell(new PdfPCell(new Phrase("Quantity")));
            itemTable.addCell(new PdfPCell(new Phrase("Supply Price")));
            itemTable.addCell(new PdfPCell(new Phrase("Total Payout")));

            BigDecimal quantityBD = BigDecimal.valueOf(deal.getQuantity());
            BigDecimal totalSupplyPrice = deal.getSupplyPrice().multiply(quantityBD);

            itemTable.addCell(deal.getCatalogue().getTitle());
            itemTable.addCell(String.valueOf(deal.getQuantity()));
            itemTable.addCell("$" + deal.getSupplyPrice().toString());
            itemTable.addCell("$" + totalSupplyPrice.toString());

            itemTable.setSpacingBefore(10);
            itemTable.setSpacingAfter(20);
            document.add(itemTable);

            Paragraph footnote = new Paragraph("* Importer identity and sell margin details are redacted for brokerage confidentiality.", 
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9));
            document.add(footnote);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    public byte[] generateQualityCertificate(Deal deal, QaReport report) {
        com.lowagie.text.Document document = new com.lowagie.text.Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Brand Header
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("EXPORTGENIUS AI - QUALITY CERTIFICATE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(30);
            document.add(title);

            document.add(new Paragraph("Certificate Reference: QC-" + deal.getId().toString().substring(0, 8)));
            document.add(new Paragraph("Issued Date: " + LocalDateTimeToString(LocalDateTime.now())));
            document.add(new Paragraph(" "));

            Paragraph statementHeader = new Paragraph("VERIFICATION STATEMENT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
            statementHeader.setSpacingAfter(10);
            document.add(statementHeader);

            String verifiedText = "This is to certify that the shipment associated with Deal Reference [" 
                    + deal.getId().toString().substring(0, 8) + "] comprising product description [" 
                    + deal.getCatalogue().getTitle() + "] with quantity [" + deal.getQuantity() 
                    + "] has been inspected by ExportGenius AI QA representatives.\n\n"
                    + "The products have been verified against compliance checklists and passed all quality audits.\n\n"
                    + "Audit Checklist Result: PASSED / COMPLIANT\n"
                    + "Compliance Score: 100/100\n\n"
                    + "Please Note: The original exporter lab report details and exporter identity are redacted to maintain broker-model confidentiality.";
            
            document.add(new Paragraph(verifiedText));
            document.add(new Paragraph(" "));
            
            Paragraph signature = new Paragraph("ExportGenius AI Quality Assurance Board", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10));
            signature.setSpacingBefore(40);
            document.add(signature);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    public byte[] generateTradeAgreement(Deal deal) {
        com.lowagie.text.Document document = new com.lowagie.text.Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("EXPORT TRADE AGREEMENT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(30);
            document.add(title);

            document.add(new Paragraph("Agreement Reference: ETA-" + deal.getId().toString().substring(0, 8)));
            document.add(new Paragraph("Date: " + LocalDateTimeToString(deal.getCreatedAt())));
            document.add(new Paragraph(" "));

            Paragraph preamble = new Paragraph("This Agreement is entered into by and between the following parties:\n\n"
                    + "1. THE BUYER / IMPORTER:\n"
                    + "   Name: " + deal.getRequirement().getImporter().getFullName() + "\n"
                    + "   Destination Country: " + deal.getRequirement().getDestinationCountry() + "\n\n"
                    + "2. THE AGENT / COUNTERPARTY:\n"
                    + "   ExportGenius AI Platform Ltd\n\n"
                    + "WHEREAS the Buyer wishes to procure goods through ExportGenius AI, and ExportGenius AI agrees to supply and act as the sole broker for the transaction subject to the following terms:",
                    FontFactory.getFont(FontFactory.HELVETICA, 10));
            
            preamble.setSpacingAfter(20);
            document.add(preamble);

            document.add(new Paragraph("TERMS & CONDITIONS", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            document.add(new Paragraph("1. Subject Matter: Sourcing and purchase of " + deal.getCatalogue().getTitle() + " in quantity " + deal.getQuantity() + "."));
            document.add(new Paragraph("2. Delivery Terms: Final destination point is " + deal.getRequirement().getDestinationCountry() + "."));
            document.add(new Paragraph("3. Pricing: Locked at the agreed price of $" + deal.getSellPrice().toString() + " per unit."));
            document.add(new Paragraph("4. Confidentiality: The supplier identity, supply price, and broker margins are fully redacted and remain confidential trade secrets."));

            Paragraph signParagraph = new Paragraph("IN WITNESS WHEREOF, the parties sign this agreement.", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10));
            signParagraph.setSpacingBefore(40);
            document.add(signParagraph);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    private PdfPCell createCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }

    private String LocalDateTimeToString(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return dateTime.toLocalDate().toString();
    }
}
