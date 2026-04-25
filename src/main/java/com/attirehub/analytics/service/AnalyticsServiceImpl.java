package com.attirehub.analytics.service;

import com.attirehub.analytics.dto.SalesTrendPointResponse;
import com.attirehub.order.repository.OrderRepository;
import com.attirehub.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SalesTrendPointResponse> getSalesTrend(String fromYearMonth, String toYearMonth) {
        YearMonth from = parseYearMonth(fromYearMonth, "from");
        YearMonth to = parseYearMonth(toYearMonth, "to");
        if (to.isBefore(from)) {
            throw new BadRequestException("'to' must be the same as or after 'from'");
        }

        LocalDateTime fromStart = from.atDay(1).atStartOfDay();
        LocalDateTime toExclusive = to.plusMonths(1).atDay(1).atStartOfDay();

        Map<String, SalesTrendPointResponse> result = new LinkedHashMap<>();
        YearMonth current = from;
        while (!current.isAfter(to)) {
            String monthKey = current.format(YM);
            result.put(monthKey, SalesTrendPointResponse.builder()
                    .month(monthKey)
                    .grossSales(BigDecimal.ZERO)
                    .orderCount(0)
                    .build());
            current = current.plusMonths(1);
        }

        List<Object[]> rows = orderRepository.fetchMonthlySalesTrend(fromStart, toExclusive);
        for (Object[] row : rows) {
            String month = row[0] != null ? row[0].toString() : null;
            if (month == null || !result.containsKey(month)) continue;
            BigDecimal grossSales = parseDecimal(row[1]);
            long orderCount = parseLong(row[2]);
            result.put(month, SalesTrendPointResponse.builder()
                    .month(month)
                    .grossSales(grossSales)
                    .orderCount(orderCount)
                    .build());
        }

        return new ArrayList<>(result.values());
    }

    private YearMonth parseYearMonth(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("'" + fieldName + "' is required (format: yyyy-MM)");
        }
        try {
            return YearMonth.parse(value, YM);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid '" + fieldName + "' format. Use yyyy-MM");
        }
    }

    private BigDecimal parseDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        return new BigDecimal(value.toString());
    }

    private long parseLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number n) return n.longValue();
        return Long.parseLong(value.toString());
    }
}
