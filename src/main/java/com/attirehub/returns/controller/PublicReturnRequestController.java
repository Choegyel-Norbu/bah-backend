package com.attirehub.returns.controller;

import com.attirehub.returns.dto.CreateReturnRequestDto;
import com.attirehub.returns.service.ReturnRequestService;
import com.attirehub.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public intake for return/refund requests (PRD Flow 5, step 1).
 */
@RestController
@RequestMapping("/api/v1/order-return-requests")
@RequiredArgsConstructor
public class PublicReturnRequestController {

    private final ReturnRequestService returnRequestService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submit(@Valid @RequestBody CreateReturnRequestDto request) {
        returnRequestService.submitPublic(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Return request submitted", null));
    }
}
