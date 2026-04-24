package com.cybermantra.microservices.in.PaymentService.repository;

import com.cybermantra.microservices.in.PaymentService.entity.Order;
import com.cybermantra.microservices.in.PaymentService.enums.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("OrderRepository Tests")
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    private UUID userId;
    private Order pendingOrder;
    private Order completedOrder;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        pendingOrder = entityManager.persistAndFlush(Order.builder()
                .orderNumber("ORD-PENDING-001").userId(userId).courseId(101L)
                .amount(new BigDecimal("999.00")).currency("USD")
                .discountAmount(BigDecimal.ZERO).finalAmount(new BigDecimal("999.00"))
                .status(OrderStatus.PENDING).build());

        completedOrder = entityManager.persistAndFlush(Order.builder()
                .orderNumber("ORD-COMPLETED-001").userId(userId).courseId(202L)
                .amount(new BigDecimal("999.00")).currency("USD")
                .discountAmount(BigDecimal.ZERO).finalAmount(new BigDecimal("999.00"))
                .status(OrderStatus.COMPLETED).completedAt(LocalDateTime.now()).build());
    }

    @Test
    @DisplayName("Should find order by order number")
    void shouldFindOrderByOrderNumber() {
        Optional<Order> found = orderRepository.findByOrderNumber("ORD-PENDING-001");
        assertThat(found).isPresent();
        assertThat(found.get().getCourseId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("Should return empty when order number not found")
    void shouldReturnEmptyWhenOrderNumberNotFound() {
        Optional<Order> found = orderRepository.findByOrderNumber("ORD-NONEXISTENT");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find all orders by userId")
    void shouldFindAllOrdersByUserId() {
        List<Order> orders = orderRepository.findAllByUserId(userId);
        assertThat(orders).hasSize(2);
        assertThat(orders).extracting(Order::getUserId).containsOnly(userId);
    }

    @Test
    @DisplayName("Should return empty list for unknown userId")
    void shouldReturnEmptyListForUnknownUserId() {
        List<Order> orders = orderRepository.findAllByUserId(UUID.randomUUID());
        assertThat(orders).isEmpty();
    }

    @Test
    @DisplayName("Should find orders by userId and status")
    void shouldFindOrdersByUserIdAndStatus() {
        List<Order> pending = orderRepository.findAllByUserIdAndStatus(userId, OrderStatus.PENDING);
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getOrderNumber()).isEqualTo("ORD-PENDING-001");

        List<Order> completed = orderRepository.findAllByUserIdAndStatus(userId, OrderStatus.COMPLETED);
        assertThat(completed).hasSize(1);
        assertThat(completed.get(0).getOrderNumber()).isEqualTo("ORD-COMPLETED-001");
    }

    @Test
    @DisplayName("Should return true when completed order exists for user and course")
    void shouldReturnTrueWhenCompletedOrderExists() {
        boolean exists = orderRepository.existsByUserIdAndCourseIdAndStatus(userId, 202L, OrderStatus.COMPLETED);
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when no completed order for user and course")
    void shouldReturnFalseWhenNoCompletedOrderExists() {
        boolean exists = orderRepository.existsByUserIdAndCourseIdAndStatus(userId, 101L, OrderStatus.COMPLETED);
        assertThat(exists).isFalse(); // 101L is PENDING not COMPLETED
    }
}