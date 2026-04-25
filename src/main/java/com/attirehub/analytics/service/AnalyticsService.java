package com.attirehub.analytics.service;

import com.attirehub.analytics.dto.SalesTrendPointResponse;

import java.util.List;

public interface AnalyticsService {
    List<SalesTrendPointResponse> getSalesTrend(String fromYearMonth, String toYearMonth);
}
