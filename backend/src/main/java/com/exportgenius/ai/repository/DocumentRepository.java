package com.exportgenius.ai.repository;

import com.exportgenius.ai.entity.Deal;
import com.exportgenius.ai.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByDeal(Deal deal);
    List<Document> findByDealAndDocumentTypeIn(Deal deal, List<String> documentTypes);
}
