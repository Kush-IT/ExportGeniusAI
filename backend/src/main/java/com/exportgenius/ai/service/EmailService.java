package com.exportgenius.ai.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            System.out.println("Asynchronous HTML email successfully dispatched to " + to);
        } catch (Exception e) {
            System.err.println("Failed to dispatch asynchronous email to " + to + ": " + e.getMessage());
        }
    }

    @Async
    public void sendVerificationEmail(String to, String token) {
        String verificationLink = "http://localhost:8080/api/auth/verify-email?token=" + token;
        String content = "<h3>Welcome to ExportGenius AI</h3>"
                + "<p>Please verify your email address by clicking the link below:</p>"
                + "<p><a href=\"" + verificationLink + "\">Verify Account</a></p>";
        sendHtmlEmail(to, "Email Verification - ExportGenius AI", content);
    }

    @Async
    public void sendDealQuotedEmail(String importerEmail, BigDecimal sellPrice) {
        String content = "<h3>New Quote Received</h3>"
                + "<p>ExportGenius AI has generated a new price quote for your requirement.</p>"
                + "<p>Proposed Unit Price: <strong>$" + sellPrice + "</strong></p>"
                + "<p>Log in to your dashboard to review and accept the offer.</p>";
        sendHtmlEmail(importerEmail, "Trade Deal Price Quote - ExportGenius AI", content);
    }

    @Async
    public void sendDealConfirmedEmail(String exporterEmail, String importerEmail, BigDecimal supplyPrice, BigDecimal sellPrice) {
        // Exporter confirmed notification (remains silent about importer identities or sellPrice)
        String exporterContent = "<h3>Deal Confirmed</h3>"
                + "<p>Your catalogue listing has been matched and confirmed by the broker.</p>"
                + "<p>Agreed Unit Payout: <strong>$" + supplyPrice + "</strong></p>"
                + "<p>Please upload shipping documentation to mark dispatch.</p>";
        sendHtmlEmail(exporterEmail, "Order Confirmed - ExportGenius AI", exporterContent);

        // Importer confirmed notification (remains silent about exporter identities or supplyPrice)
        String importerContent = "<h3>Deal Confirmed</h3>"
                + "<p>Your sourcing requirement has been successfully finalized.</p>"
                + "<p>Agreed Unit Price: <strong>$" + sellPrice + "</strong></p>"
                + "<p>Please complete your payment order in the dashboard.</p>";
        sendHtmlEmail(importerEmail, "Order Confirmed - ExportGenius AI", importerContent);
    }

    @Async
    public void sendDispatchNotificationEmail(String importerEmail, String docUrl) {
        String content = "<h3>Order Dispatched</h3>"
                + "<p>Your shipment has been dispatched. The shipping document is attached below:</p>"
                + "<p><a href=\"" + docUrl + "\">View Bill of Lading</a></p>";
        sendHtmlEmail(importerEmail, "Shipment Dispatched - ExportGenius AI", content);
    }

    @Async
    public void sendPayoutSettledEmail(String exporterEmail, String bankRef) {
        String content = "<h3>Payout Settled</h3>"
                + "<p>Your payout has been processed successfully.</p>"
                + "<p>Bank Settlement Reference: <strong>" + bankRef + "</strong></p>";
        sendHtmlEmail(exporterEmail, "Payout Settled - ExportGenius AI", content);
    }
}
