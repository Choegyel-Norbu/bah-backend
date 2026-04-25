package com.attirehub.referral.controller;

import com.attirehub.referral.dto.ReferralClickRequest;
import com.attirehub.referral.service.ReferralService;
import com.attirehub.shared.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public referral tracking (PRD Flow 2, step 3).
 */
@RestController
@RequestMapping("/api/v1/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    @PostMapping("/click")
    public ResponseEntity<ApiResponse<Void>> click(
            @Valid @RequestBody ReferralClickRequest body,
            HttpServletRequest request) {
        referralService.recordClick(body, request);
        return ResponseEntity.ok(ApiResponse.success("OK"));
    }
}
