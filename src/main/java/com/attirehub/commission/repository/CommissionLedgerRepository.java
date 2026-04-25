package com.attirehub.commission.repository;

import com.attirehub.commission.entity.CommissionLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommissionLedgerRepository extends JpaRepository<CommissionLedgerEntry, Long> {
}
