package com.attirehub.partner.entity;

import com.attirehub.partner.enums.PartnerStatus;
import com.attirehub.partner.enums.PartnerType;
import com.attirehub.shared.entity.BaseEntity;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "partners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Partner extends BaseEntity {

    @Column(name = "referral_code", nullable = false, unique = true, length = 100)
    private String referralCode;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "partner_type", nullable = false, length = 20, columnDefinition = "varchar(20)")
    @Builder.Default
    private PartnerType partnerType = PartnerType.HOTEL;

    @Column(name = "email")
    private String email;

    @Column(name = "commission_rate_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal commissionRatePercent = new BigDecimal("5.00");

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
    @Builder.Default
    private PartnerStatus status = PartnerStatus.ACTIVE;

    @Column(name = "total_clicks", nullable = false)
    @Builder.Default
    private int totalClicks = 0;
}
