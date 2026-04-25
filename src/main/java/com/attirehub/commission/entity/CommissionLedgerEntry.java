package com.attirehub.commission.entity;

import com.attirehub.commission.enums.CommissionEntryType;
import com.attirehub.commission.enums.CommissionStakeholder;
import com.attirehub.order.entity.Order;
import com.attirehub.order.entity.OrderItem;
import com.attirehub.partner.entity.Partner;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "commission_ledger_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "stakeholder", length = 30, columnDefinition = "varchar(30)")
    private CommissionStakeholder stakeholder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    private Partner partner;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20, columnDefinition = "varchar(20)")
    private CommissionEntryType entryType;

    @Column(name = "amount_btn", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountBtn;

    @Column(name = "commission_rate_snapshot", precision = 10, scale = 6)
    private BigDecimal commissionRateSnapshot;

    @Column(name = "base_amount_btn", precision = 12, scale = 2)
    private BigDecimal baseAmountBtn;

    @Column(length = 512)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
