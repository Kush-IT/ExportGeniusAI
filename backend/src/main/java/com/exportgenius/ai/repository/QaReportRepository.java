package com.exportgenius.ai.repository;

import com.exportgenius.ai.entity.ExporterCatalogue;
import com.exportgenius.ai.entity.QaReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QaReportRepository extends JpaRepository<QaReport, UUID> {
    List<QaReport> findByCatalogue(ExporterCatalogue catalogue);
    // Find the latest QA report for a catalogue item that has passed
    Optional<QaReport> findFirstByCatalogueAndPassedTrueOrderByCreatedAtDesc(ExporterCatalogue catalogue);
}
