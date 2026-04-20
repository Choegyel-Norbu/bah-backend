package com.attirehub.user.entity;

import com.attirehub.shared.entity.BaseEntity;
import com.attirehub.shared.enums.AddressType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Address entity linked to a user.
 * Supports both SHIPPING and BILLING address types.
 */
@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, columnDefinition = "varchar(20)")
    @Builder.Default
    private AddressType addressType = AddressType.SHIPPING;

    @Column(name = "street_address", nullable = false)
    private String streetAddress;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;
}
