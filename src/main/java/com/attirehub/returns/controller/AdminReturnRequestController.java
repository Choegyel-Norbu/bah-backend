package com.attirehub.returns.controller;

import com.attirehub.returns.dto.ReturnRequestResponse;
import com.attirehub.returns.service.ReturnRequestService;
import com.attirehub.shared.dto.ApiResponse;
import com.attirehub.shared.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/return-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReturnRequestController {

    private final ReturnRequestService returnRequestService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ReturnRequestResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(returnRequestService.listForAdmin(page, size)));
    }
}
