package com.attirehub.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    private Long id;
    private Long variantId;
    private String productName;
    private String sku;
    private String size;
    private String color;
    private BigDecimal unitPrice;
    private BigDecimal discount;
    private int quantity;
    private BigDecimal totalPrice;
    private String imageUrl;
    private int availableStock;
}
