package com.cybermantra.microservices.in.PaymentService.repository;

import com.cybermantra.microservices.in.PaymentService.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    Optional<Coupon> findByCode(String code);

    Optional<Coupon> findByCodeAndIsActiveTrue(String code);

    // Find valid coupon - active, within date range, and not exhausted
    @Query("SELECT c FROM Coupon c WHERE c.code = :code " +
            "AND c.isActive = true " +
            "AND c.validFrom <= :now " +
            "AND c.validUntil >= :now " +
            "AND (c.maxUses IS NULL OR c.currentUses < c.maxUses)")
    Optional<Coupon> findValidCoupon(String code, LocalDateTime now);
}
