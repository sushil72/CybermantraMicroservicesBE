package com.cybermantra.microservices.in.PaymentService.repository;
import com.cybermantra.microservices.in.PaymentService.entity.Order;
import com.cybermantra.microservices.in.PaymentService.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findAllByUserId(UUID userId);

    List<Order> findAllByUserIdAndStatus(UUID userId, OrderStatus status);

    boolean existsByUserIdAndCourseIdAndStatus(UUID userId, Long courseId, OrderStatus status);

    // For revenue tracking - sum of completed orders in a period
    @Query("SELECT SUM(o.finalAmount) FROM Order o WHERE o.status = 'COMPLETED' " +
            "AND o.completedAt BETWEEN :from AND :to")
    BigDecimal sumCompletedOrdersBetween(LocalDateTime from, LocalDateTime to);
}
