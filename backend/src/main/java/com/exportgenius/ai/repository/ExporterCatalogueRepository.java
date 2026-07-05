package com.exportgenius.ai.repository;

import com.exportgenius.ai.entity.ExporterCatalogue;
import com.exportgenius.ai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExporterCatalogueRepository extends JpaRepository<ExporterCatalogue, UUID> {
    List<ExporterCatalogue> findByExporterAndIsActiveTrue(User exporter);
    Optional<ExporterCatalogue> findByIdAndExporter(UUID id, User exporter);
}
