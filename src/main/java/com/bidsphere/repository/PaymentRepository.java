package com.bidsphere.repository;

import com.bidsphere.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findFirstByBidIdOrderByPaymentTimeDesc(Long bidId);
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.auction.seller.id = :sellerId AND p.status = 'PENDING'")
    long countPendingPaymentsForSeller(@Param("sellerId") Long sellerId);
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.auction.seller.id = :sellerId AND p.status = 'PAID'")
    long countCompletedPaymentsForSeller(@Param("sellerId") Long sellerId);
    
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.auction.seller.id = :sellerId AND p.status = 'PAID'")
    BigDecimal getTotalPaidAmountForSeller(@Param("sellerId") Long sellerId);
}
