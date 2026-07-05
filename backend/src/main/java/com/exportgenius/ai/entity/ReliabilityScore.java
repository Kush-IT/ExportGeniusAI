package com.exportgenius.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reliability_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReliabilityScore {
    @Id
    @Column(name = "exporter_id")
    private UUID exporterId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "exporter_id")
    private User exporter;

    @Column(name = "qa_pass_pct", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal qaPassPct = BigDecimal.valueOf(100.00);

    @Column(name = "on_time_pct", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal onTimePct = BigDecimal.valueOf(100.00);

    @Column(name = "dispute_count", nullable = false)
    @Builder.Default
    private Integer disputeCount = 0;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
