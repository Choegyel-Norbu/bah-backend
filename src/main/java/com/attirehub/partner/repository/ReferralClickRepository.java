package com.attirehub.partner.repository;

import com.attirehub.partner.entity.ReferralClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ReferralClickRepository extends JpaRepository<ReferralClick, Long> {

    @Query("SELECT COUNT(r) FROM ReferralClick r WHERE r.ipHash = :ipHash AND r.createdAt >= :since")
    long countByIpHashSince(@Param("ipHash") String ipHash, @Param("since") LocalDateTime since);
}
