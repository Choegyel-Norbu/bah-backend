package com.attirehub.inventory.repository;

import com.attirehub.inventory.entity.StockAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockAuditLogRepository extends JpaRepository<StockAuditLog, Long> {
}
