package com.attirehub.returns.service;

import com.attirehub.order.entity.Order;
import com.attirehub.order.entity.OrderItem;
import com.attirehub.order.repository.OrderRepository;
import com.attirehub.returns.dto.CreateReturnRequestDto;
import com.attirehub.returns.dto.ReturnRequestResponse;
import com.attirehub.returns.entity.OrderReturnRequest;
import com.attirehub.returns.repository.OrderReturnRequestRepository;
import com.attirehub.shared.dto.PagedResponse;
import com.attirehub.shared.enums.OrderStatus;
import com.attirehub.shared.exception.BadRequestException;
import com.attirehub.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReturnRequestServiceImpl implements ReturnRequestService {

    private final OrderRepository orderRepository;
    private final OrderReturnRequestRepository orderReturnRequestRepository;

    @Override
    @Transactional
    public void submitPublic(CreateReturnRequestDto request) {
        Order order = orderRepository.findByOrderNumber(request.getOrderNumber().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", request.getOrderNumber()));

        if (!order.getUser().getEmail().equalsIgnoreCase(request.getEmail().trim())) {
            throw new BadRequestException("Email does not match this order");
        }

        if (order.getStatus() != OrderStatus.DELIVERED
                && order.getStatus() != OrderStatus.SHIPPED
                && order.getStatus() != OrderStatus.COMPLETED) {
            throw new BadRequestException("Returns can only be requested for shipped or delivered orders");
        }

        Set<Long> allowed = order.getItems().stream()
                .map(OrderItem::getVariant)
                .map(v -> v.getId())
                .collect(Collectors.toSet());
        for (Long variantId : request.getVariantIds()) {
            if (!allowed.contains(variantId)) {
                throw new BadRequestException("Variant " + variantId + " is not part of this order");
            }
        }

        String ids = request.getVariantIds().stream().map(String::valueOf).collect(Collectors.joining(","));

        OrderReturnRequest entity = OrderReturnRequest.builder()
                .order(order)
                .customerEmail(request.getEmail().trim())
                .reason(request.getReason())
                .itemVariantIds(ids)
                .photoUrls(request.getPhotoUrls())
                .build();
        orderReturnRequestRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReturnRequestResponse> listForAdmin(int page, int size) {
        Page<OrderReturnRequest> p = orderReturnRequestRepository.findPage(PageRequest.of(page, size));
        return PagedResponse.<ReturnRequestResponse>builder()
                .content(p.getContent().stream().map(this::mapRow).toList())
                .page(p.getNumber())
                .size(p.getSize())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .last(p.isLast())
                .build();
    }

    private ReturnRequestResponse mapRow(OrderReturnRequest r) {
        return ReturnRequestResponse.builder()
                .id(r.getId())
                .orderNumber(r.getOrder().getOrderNumber())
                .customerEmail(r.getCustomerEmail())
                .reason(r.getReason())
                .itemVariantIds(r.getItemVariantIds())
                .photoUrls(r.getPhotoUrls())
                .status(r.getStatus())
                .adminNotes(r.getAdminNotes())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
