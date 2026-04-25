package com.attirehub.payment.repository;

import com.attirehub.payment.entity.PaymentRecord;
import com.attirehub.payment.enums.PaymentRecordStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    @EntityGraph(attributePaths = {
            "order",
            "order.items",
            "order.items.variant",
            "order.referralPartner",
            "order.user"
    })
    Optional<PaymentRecord> findByStripePaymentIntentId(String stripePaymentIntentId);

    Optional<PaymentRecord> findTopByOrder_IdOrderByIdDesc(Long orderId);

    boolean existsByStripePaymentIntentIdAndStatus(String stripePaymentIntentId, PaymentRecordStatus status);
}
