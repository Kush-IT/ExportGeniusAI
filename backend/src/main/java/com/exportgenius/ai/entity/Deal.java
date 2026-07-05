package com.exportgenius.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deal {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requirement_id", nullable = false)
    private Requirement requirement;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "catalogue_id", nullable = false)
    private ExporterCatalogue catalogue;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "supply_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal supplyPrice;

    @Column(name = "sell_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal sellPrice;

    @Column(name = "margin_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal marginAmount;

    @Column(name = "margin_pct", nullable = false, precision = 5, scale = 4)
    private BigDecimal marginPct;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "DEAL_STAGE", nullable = false)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private DealStage stage = DealStage.SOURCING;

    @Column(name = "importer_accepted", nullable = false)
    private boolean importerAccepted;

    @Column(name = "delivery_deadline")
    private LocalDateTime deliveryDeadline;

    @Column(name = "shipping_document_url", length = 255)
    private String shippingDocumentUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
