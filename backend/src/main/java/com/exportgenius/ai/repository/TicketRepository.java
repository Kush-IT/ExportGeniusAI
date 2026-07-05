package com.exportgenius.ai.repository;

import com.exportgenius.ai.entity.Ticket;
import com.exportgenius.ai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    List<Ticket> findByCreator(User creator);
    List<Ticket> findByStatus(String status);
    
    // Count active disputes associated with a specific exporter user
    long countByDealCatalogueExporterAndStatusIn(User exporter, List<String> statuses);
}
