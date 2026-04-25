package com.attirehub.analytics.controller;

import com.attirehub.analytics.dto.SalesTrendPointResponse;
import com.attirehub.analytics.service.AnalyticsService;
import com.attirehub.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/sales-trend")
    public ResponseEntity<ApiResponse<List<SalesTrendPointResponse>>> getSalesTrend(
            @RequestParam String from,
            @RequestParam String to) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getSalesTrend(from, to)
        ));
    }
}
