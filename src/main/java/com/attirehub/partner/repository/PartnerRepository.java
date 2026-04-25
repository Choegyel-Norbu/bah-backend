package com.attirehub.partner.repository;

import com.attirehub.partner.entity.Partner;
import com.attirehub.partner.enums.PartnerStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, Long> {

    Optional<Partner> findByReferralCodeIgnoreCase(String referralCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Partner p WHERE UPPER(p.referralCode) = UPPER(:code)")
    Optional<Partner> findByReferralCodeIgnoreCaseForUpdate(@Param("code") String referralCode);

    Optional<Partner> findByReferralCodeIgnoreCaseAndStatus(String referralCode, PartnerStatus status);
}
