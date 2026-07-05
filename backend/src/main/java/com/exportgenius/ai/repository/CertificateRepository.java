package com.exportgenius.ai.repository;

import com.exportgenius.ai.entity.Certificate;
import com.exportgenius.ai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, UUID> {
    List<Certificate> findByExporter(User exporter);
    List<Certificate> findByExpiryDateBetween(LocalDateTime start, LocalDateTime end);
}
