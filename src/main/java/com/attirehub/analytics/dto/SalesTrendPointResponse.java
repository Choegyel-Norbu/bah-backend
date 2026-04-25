package com.attirehub.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesTrendPointResponse {
    /**
     * Year-month key in yyyy-MM format.
     */
    private String month;
    private BigDecimal grossSales;
    private long orderCount;
}
