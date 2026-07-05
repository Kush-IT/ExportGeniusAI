package com.exportgenius.ai.job;

import com.exportgenius.ai.entity.*;
import com.exportgenius.ai.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ReliabilityScoreJob {

    private final UserRepository userRepository;
    private final ExporterCatalogueRepository catalogueRepository;
    private final QaReportRepository qaReportRepository;
    private final DealRepository dealRepository;
    private final TicketRepository ticketRepository;
    private final ReliabilityScoreRepository reliabilityScoreRepository;

    public ReliabilityScoreJob(UserRepository userRepository,
                               ExporterCatalogueRepository catalogueRepository,
                               QaReportRepository qaReportRepository,
                               DealRepository dealRepository,
                               TicketRepository ticketRepository,
                               ReliabilityScoreRepository reliabilityScoreRepository) {
        this.userRepository = userRepository;
        this.catalogueRepository = catalogueRepository;
        this.qaReportRepository = qaReportRepository;
        this.dealRepository = dealRepository;
        this.ticketRepository = ticketRepository;
        this.reliabilityScoreRepository = reliabilityScoreRepository;
    }

    // Run weekly at midnight on Sunday
    @Scheduled(cron = "0 0 0 * * SUN")
    @Transactional
    public void run() {
        System.out.println("Starting scheduled Reliability Score calculations...");
        executeCalculation();
        System.out.println("Finished scheduled Reliability Score calculations.");
    }

    // Public method to trigger calculation manually (e.g. for testing/admin consoles)
    @Transactional
    public void executeCalculation() {
        // Load all users who hold the EXPORTER role
        List<User> exporters = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals(Role.EXPORTER)))
                .collect(Collectors.toList());

        for (User exporter : exporters) {
            List<ExporterCatalogue> catalogItems = catalogueRepository.findByExporterAndIsActiveTrue(exporter);
            
            // 1. Calculate QA Pass Percentage
            long totalQa = 0;
            long passedQa = 0;
            for (ExporterCatalogue item : catalogItems) {
                List<QaReport> reports = qaReportRepository.findByCatalogue(item);
                totalQa += reports.size();
                passedQa += reports.stream().filter(QaReport::isPassed).count();
            }
            double qaPassPct = totalQa == 0 ? 100.0 : ((double) passedQa / totalQa) * 100.0;

            // 2. Calculate On-Time Delivery Percentage
            List<Deal> dispatchedDeals = dealRepository.findByCatalogueExporter(exporter).stream()
                    .filter(d -> d.getStage() == DealStage.DISPATCHED || d.getStage() == DealStage.PAID)
                    .collect(Collectors.toList());

            long totalDispatched = dispatchedDeals.size();
            long onTimeCount = 0;
            for (Deal deal : dispatchedDeals) {
                if (deal.getDeliveryDeadline() == null || 
                        deal.getUpdatedAt().isBefore(deal.getDeliveryDeadline()) || 
                        deal.getUpdatedAt().isEqual(deal.getDeliveryDeadline())) {
                    onTimeCount++;
                }
            }
            double onTimePct = totalDispatched == 0 ? 100.0 : ((double) onTimeCount / totalDispatched) * 100.0;

            // 3. Count active dispute tickets (status OPEN or IN_REVIEW)
            long disputeCount = ticketRepository.countByDealCatalogueExporterAndStatusIn(
                    exporter, 
                    List.of("OPEN", "IN_REVIEW")
            );

            // 4. Save/Update reliability score
            ReliabilityScore score = reliabilityScoreRepository.findById(exporter.getId())
                    .orElseGet(() -> ReliabilityScore.builder()
                            .exporterId(exporter.getId())
                            .exporter(exporter)
                            .build());

            score.setQaPassPct(BigDecimal.valueOf(qaPassPct).setScale(2, RoundingMode.HALF_UP));
            score.setOnTimePct(BigDecimal.valueOf(onTimePct).setScale(2, RoundingMode.HALF_UP));
            score.setDisputeCount((int) disputeCount);

            reliabilityScoreRepository.save(score);
        }
    }
}
