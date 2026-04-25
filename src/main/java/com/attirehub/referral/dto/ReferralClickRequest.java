package com.attirehub.referral.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralClickRequest {

    /**
     * Partner referral code (PRD Flow 2). Invalid formats are ignored server-side (still HTTP 200).
     */
    @NotBlank
    @Size(max = 100)
    private String referralCode;

    @Size(max = 128)
    private String sessionId;
}
