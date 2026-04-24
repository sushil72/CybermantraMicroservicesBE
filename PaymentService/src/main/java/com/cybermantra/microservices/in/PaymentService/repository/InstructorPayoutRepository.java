package com.cybermantra.microservices.in.PaymentService.repository;

import com.cybermantra.microservices.in.PaymentService.entity.InstructorPayout;
import com.cybermantra.microservices.in.PaymentService.enums.PayoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface InstructorPayoutRepository extends JpaRepository<InstructorPayout, UUID> {

    List<InstructorPayout> findAllByInstructorId(UUID instructorId);

    List<InstructorPayout> findAllByStatus(PayoutStatus status);

    List<InstructorPayout> findAllByInstructorIdAndStatus(UUID instructorId, PayoutStatus status);

    // Total earnings for instructor in a period
    @Query("SELECT SUM(p.amount) FROM InstructorPayout p " +
            "WHERE p.instructorId = :instructorId " +
            "AND p.status = 'PROCESSED' " +
            "AND p.periodStart >= :from " +
            "AND p.periodEnd <= :to")
    BigDecimal sumProcessedPayouts(UUID instructorId, LocalDate from, LocalDate to);

    // Check if payout already exists for this period
    boolean existsByInstructorIdAndPeriodStartAndPeriodEnd(
            UUID instructorId, LocalDate periodStart, LocalDate periodEnd);
}
