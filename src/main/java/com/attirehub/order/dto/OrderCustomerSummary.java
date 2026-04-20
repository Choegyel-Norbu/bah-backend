package com.attirehub.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Customer details included in admin order responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCustomerSummary {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
}
