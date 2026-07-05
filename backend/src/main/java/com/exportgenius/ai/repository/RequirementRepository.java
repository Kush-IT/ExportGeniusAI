package com.exportgenius.ai.repository;

import com.exportgenius.ai.entity.Requirement;
import com.exportgenius.ai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RequirementRepository extends JpaRepository<Requirement, UUID> {
    List<Requirement> findByImporter(User importer);
    Optional<Requirement> findByIdAndImporter(UUID id, User importer);
}
