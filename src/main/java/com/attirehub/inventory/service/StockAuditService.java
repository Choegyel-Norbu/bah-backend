package com.attirehub.inventory.service;

import com.attirehub.inventory.entity.StockAuditLog;
import com.attirehub.inventory.enums.StockChangeType;
import com.attirehub.inventory.repository.StockAuditLogRepository;
import com.attirehub.order.entity.Order;
import com.attirehub.product.entity.ProductVariant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockAuditService {

    private final StockAuditLogRepository stockAuditLogRepository;

    @Transactional
    public void log(
            ProductVariant variant,
            StockChangeType changeType,
            int quantityDelta,
            Integer availableAfter,
            Integer reservedAfter,
            Order order,
            String reason) {
        stockAuditLogRepository.save(StockAuditLog.builder()
                .variant(variant)
                .changeType(changeType)
                .quantityDelta(quantityDelta)
                .availableAfter(availableAfter)
                .reservedAfter(reservedAfter)
                .order(order)
                .reason(reason)
                .build());
    }
}
