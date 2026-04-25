package com.attirehub.referral.service;

import com.attirehub.referral.dto.ReferralClickRequest;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Referral click tracking (PRD Flow 2). Always succeeds from the client's perspective.
 */
public interface ReferralService {

    void recordClick(ReferralClickRequest body, HttpServletRequest request);
}
