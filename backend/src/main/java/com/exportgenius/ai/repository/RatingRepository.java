package com.exportgenius.ai.repository;

import com.exportgenius.ai.entity.Deal;
import com.exportgenius.ai.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID> {
    Optional<Rating> findByDeal(Deal deal);
}
