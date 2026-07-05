package com.exportgenius.ai.job;

import com.exportgenius.ai.entity.*;
import com.exportgenius.ai.repository.*;
import com.exportgenius.ai.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CertificationExpiryJob {

    private final CertificateRepository certificateRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public CertificationExpiryJob(CertificateRepository certificateRepository,
                                  UserRepository userRepository,
                                  NotificationService notificationService) {
        this.certificateRepository = certificateRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    // Run daily at 1:00 AM
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void run() {
        System.out.println("Running daily Certifications monitor...");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysFromNow = now.plusDays(30);
        
        List<Certificate> expiringCerts = certificateRepository.findByExpiryDateBetween(now, thirtyDaysFromNow);
        
        if (expiringCerts.isEmpty()) {
            return;
        }

        // Get all system admins
        List<User> admins = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals(Role.ADMIN)))
                .collect(Collectors.toList());

        for (Certificate cert : expiringCerts) {
            String title = "Certificate Expiring Soon";
            String message = "Exporter (" + cert.getExporter().getFullName() + ") certificate '" 
                    + cert.getCertificateType() + "' is expiring on " + cert.getExpiryDate().toLocalDate().toString() + ".";
            
            for (User admin : admins) {
                notificationService.create(
                        admin,
                        title,
                        message,
                        "CERTIFICATE_EXPIRY",
                        "/admin/exporters/" + cert.getExporter().getId()
                );
            }
        }
        
        System.out.println("Certification monitor execution complete. Flagged " + expiringCerts.size() + " certificates.");
    }
}
