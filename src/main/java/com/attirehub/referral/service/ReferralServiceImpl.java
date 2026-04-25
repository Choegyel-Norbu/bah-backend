package com.attirehub.referral.service;

import com.attirehub.partner.entity.Partner;
import com.attirehub.partner.entity.ReferralClick;
import com.attirehub.partner.enums.PartnerStatus;
import com.attirehub.partner.repository.PartnerRepository;
import com.attirehub.partner.repository.ReferralClickRepository;
import com.attirehub.referral.dto.ReferralClickRequest;
import com.attirehub.shared.util.HashUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReferralServiceImpl implements ReferralService {

    private static final Logger log = LoggerFactory.getLogger(ReferralServiceImpl.class);
    /** PRD 04 fraud — max clicks per hashed IP per 24h */
    private static final int MAX_CLICKS_PER_IP_PER_DAY = 100;

    private final PartnerRepository partnerRepository;
    private final ReferralClickRepository referralClickRepository;

    private static final String REFERRAL_CODE_PATTERN = "^[A-Z][A-Z0-9]*_[A-Z0-9_]+$";

    @Override
    @Transactional
    public void recordClick(ReferralClickRequest body, HttpServletRequest request) {
        String code = body.getReferralCode().trim().toUpperCase();
        if (!code.matches(REFERRAL_CODE_PATTERN)) {
            log.debug("Referral click ignored: invalid code format");
            return;
        }
        Partner partner = partnerRepository.findByReferralCodeIgnoreCaseForUpdate(code)
                .filter(p -> p.getStatus() == PartnerStatus.ACTIVE)
                .orElse(null);
        if (partner == null) {
            log.debug("Referral click ignored: no active partner for code={}", code);
            return;
        }

        String userAgent = request.getHeader("User-Agent");
        String ip = request.getRemoteAddr();
        String ipHash = HashUtils.sha256Hex(ip);
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        if (ipHash != null
                && referralClickRepository.countByIpHashSince(ipHash, since) >= MAX_CLICKS_PER_IP_PER_DAY) {
            log.warn("Referral click rate limit exceeded (hashed IP)");
            return;
        }

        ReferralClick click = ReferralClick.builder()
                .partner(partner)
                .referralCode(partner.getReferralCode())
                .userAgentHash(HashUtils.sha256Hex(userAgent))
                .ipHash(ipHash)
                .sessionId(body.getSessionId())
                .build();
        referralClickRepository.save(click);

        partner.setTotalClicks(partner.getTotalClicks() + 1);
        partnerRepository.save(partner);

        log.debug("Referral click recorded: partnerId={}, code={}", partner.getId(), partner.getReferralCode());
    }
}
