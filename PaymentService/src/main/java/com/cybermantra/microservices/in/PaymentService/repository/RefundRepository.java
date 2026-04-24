package com.cybermantra.microservices.in.PaymentService.repository;

import com.cybermantra.microservices.in.PaymentService.entity.Refund;
import com.cybermantra.microservices.in.PaymentService.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {

    List<Refund> findAllByOrderId(UUID orderId);

    List<Refund> findAllByRequestedBy(UUID userId);

    List<Refund> findAllByStatus(RefundStatus status);

    // Check if refund already requested for this order
    boolean existsByOrderIdAndStatusNot(UUID orderId, RefundStatus status);
}
