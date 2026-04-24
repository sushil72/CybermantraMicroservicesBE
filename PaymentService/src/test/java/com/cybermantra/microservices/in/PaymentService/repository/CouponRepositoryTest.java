package com.cybermantra.microservices.in.PaymentService.repository;

import com.cybermantra.microservices.in.PaymentService.entity.Coupon;
import com.cybermantra.microservices.in.PaymentService.enums.DiscountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("CouponRepository Tests")
class CouponRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CouponRepository couponRepository;

    @BeforeEach
    void setUp() {
        // Active valid coupon
        entityManager.persistAndFlush(Coupon.builder()
                .code("VALID20").discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20")).maxUses(100).currentUses(0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(30))
                .isActive(true).build());

        // Expired coupon
        entityManager.persistAndFlush(Coupon.builder()
                .code("EXPIRED10").discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10")).maxUses(100).currentUses(0)
                .validFrom(LocalDateTime.now().minusDays(60))
                .validUntil(LocalDateTime.now().minusDays(30))
                .isActive(true).build());

        // Exhausted coupon (max uses reached)
        entityManager.persistAndFlush(Coupon.builder()
                .code("EXHAUSTED").discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("50")).maxUses(10).currentUses(10)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(30))
                .isActive(true).build());

        // Inactive coupon
        entityManager.persistAndFlush(Coupon.builder()
                .code("INACTIVE").discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("50")).currentUses(0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(30))
                .isActive(false).build());
    }

    @Test
    @DisplayName("Should find valid coupon by code")
    void shouldFindValidCouponByCode() {
        Optional<Coupon> coupon = couponRepository.findValidCoupon("VALID20", LocalDateTime.now());
        assertThat(coupon).isPresent();
        assertThat(coupon.get().getCode()).isEqualTo("VALID20");
        assertThat(coupon.get().getDiscountValue()).isEqualByComparingTo(new BigDecimal("20"));
    }

    @Test
    @DisplayName("Should not find expired coupon")
    void shouldNotFindExpiredCoupon() {
        Optional<Coupon> coupon = couponRepository.findValidCoupon("EXPIRED10", LocalDateTime.now());
        assertThat(coupon).isEmpty();
    }

    @Test
    @DisplayName("Should not find exhausted coupon")
    void shouldNotFindExhaustedCoupon() {
        Optional<Coupon> coupon = couponRepository.findValidCoupon("EXHAUSTED", LocalDateTime.now());
        assertThat(coupon).isEmpty();
    }

    @Test
    @DisplayName("Should not find inactive coupon")
    void shouldNotFindInactiveCoupon() {
        Optional<Coupon> coupon = couponRepository.findValidCoupon("INACTIVE", LocalDateTime.now());
        assertThat(coupon).isEmpty();
    }

    @Test
    @DisplayName("Should find coupon by code")
    void shouldFindCouponByCode() {
        Optional<Coupon> coupon = couponRepository.findByCode("VALID20");
        assertThat(coupon).isPresent();
    }

    @Test
    @DisplayName("Should not find coupon with non-existent code")
    void shouldNotFindCouponWithNonExistentCode() {
        Optional<Coupon> coupon = couponRepository.findByCode("DOESNOTEXIST");
        assertThat(coupon).isEmpty();
    }
}