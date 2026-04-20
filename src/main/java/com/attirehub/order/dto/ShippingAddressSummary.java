package com.attirehub.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shipping address summary for order responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddressSummary {

    private String streetAddress;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}
