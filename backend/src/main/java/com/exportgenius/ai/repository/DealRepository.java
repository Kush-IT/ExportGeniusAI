package com.exportgenius.ai.repository;

import com.exportgenius.ai.entity.Deal;
import com.exportgenius.ai.entity.DealStage;
import com.exportgenius.ai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DealRepository extends JpaRepository<Deal, UUID> {
    List<Deal> findByCatalogueExporter(User exporter);
    List<Deal> findByRequirementImporter(User importer);
    List<Deal> findByStage(DealStage stage);
    Optional<Deal> findByIdAndCatalogueExporter(UUID id, User exporter);
    Optional<Deal> findByIdAndRequirementImporter(UUID id, User importer);
}
