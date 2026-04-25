package com.attirehub.returns.dto;

import com.attirehub.returns.enums.OrderReturnReason;
import com.attirehub.returns.enums.OrderReturnRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestResponse {

    private Long id;
    private String orderNumber;
    private String customerEmail;
    private OrderReturnReason reason;
    private String itemVariantIds;
    private String photoUrls;
    private OrderReturnRequestStatus status;
    private String adminNotes;
    private LocalDateTime createdAt;
}
