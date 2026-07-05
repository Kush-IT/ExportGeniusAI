package com.exportgenius.ai.service;

import com.exportgenius.ai.entity.*;
import com.exportgenius.ai.repository.AuditLogRepository;
import com.exportgenius.ai.repository.DealRepository;
import com.exportgenius.ai.repository.UserRepository;
import com.exportgenius.ai.validator.DealStageValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DealStageService {

    private final DealRepository dealRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final DealStageValidator dealStageValidator;
    private final StageEffectHandler stageEffectHandler;

    public DealStageService(DealRepository dealRepository,
                            UserRepository userRepository,
                            AuditLogRepository auditLogRepository,
                            DealStageValidator dealStageValidator,
                            StageEffectHandler stageEffectHandler) {
        this.dealRepository = dealRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.dealStageValidator = dealStageValidator;
        this.stageEffectHandler = stageEffectHandler;
    }

    @Transactional
    public Deal advanceStage(UUID dealId, DealStage targetStage, String userEmail) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new IllegalArgumentException("Deal not found"));

        User actor = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Actor profile not found"));

        DealStage previousStage = deal.getStage();

        // 1. Run entry guards / validation rules
        dealStageValidator.validate(deal, targetStage);

        // 2. Transition state
        deal.setStage(targetStage);
        
        // 3. Apply stage entry side effects
        stageEffectHandler.handle(deal, targetStage);

        // 4. Save updated deal
        Deal savedDeal = dealRepository.save(deal);

        // 5. Create audit logs record
        AuditLog auditLog = AuditLog.builder()
                .user(actor)
                .action("STAGE_TRANSITION")
                .details("Advanced deal from " + previousStage + " to " + targetStage + " by " + actor.getEmail())
                .build();
        auditLogRepository.save(auditLog);

        return savedDeal;
    }
}
