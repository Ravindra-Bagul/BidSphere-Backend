package com.bidsphere.service.impl;

import com.bidsphere.entity.*;
import com.bidsphere.repository.AuctionRepository;
import com.bidsphere.repository.BidRepository;
import com.bidsphere.repository.PaymentRepository;
import com.bidsphere.service.PaymentService;
import com.bidsphere.util.RazorpayUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final RazorpayUtil razorpayUtil;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                            BidRepository bidRepository,
                            AuctionRepository auctionRepository,
                            RazorpayUtil razorpayUtil) {
        this.paymentRepository = paymentRepository;
        this.bidRepository = bidRepository;
        this.auctionRepository = auctionRepository;
        this.razorpayUtil = razorpayUtil;
    }

    @Override
    @Transactional
    public Map<String, Object> createPaymentOrder(Long bidId) {
        try {
            Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new RuntimeException("Bid not found"));

            // Create or get existing payment
            Optional<Payment> existingPayment = paymentRepository.findFirstByBidIdOrderByPaymentTimeDesc(bidId);
            Payment payment = existingPayment.orElse(new Payment());
            
            payment.setBid(bid);
            payment.setAuction(bid.getAuction());
            payment.setAmount(bid.getAmount());
            payment.setStatus(PaymentStatus.PENDING);
            payment.setPaymentTime(LocalDateTime.now());
            payment = paymentRepository.save(payment);

            // Create Razorpay order
            Map<String, Object> orderData = razorpayUtil.createOrder(
                bid.getAmount().multiply(BigDecimal.valueOf(100)).longValue(),
                "INR",
                "bid_" + bidId
            );

            // Update payment with order ID
            payment.setOrderId((String) orderData.get("orderId"));
            paymentRepository.save(payment);

            return orderData;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create payment order: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Map<String, Object> verifyAndUpdatePayment(Long bidId, String orderId, 
                                                     String paymentId, String signature) {
        if (!razorpayUtil.verifySignature(orderId, paymentId, signature)) {
            throw new RuntimeException("Invalid payment signature");
        }

        // Update payment status
        Payment payment = paymentRepository.findFirstByBidIdOrderByPaymentTimeDesc(bidId)
            .orElseThrow(() -> new RuntimeException("Payment not found for bid: " + bidId));

        payment.setPaymentId(paymentId);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaymentTime(LocalDateTime.now());
        paymentRepository.save(payment);

        // Update auction status
        Auction auction = payment.getAuction();
        auction.setWinningBid(payment.getBid());
        auction.setStatus(AuctionStatus.ENDED);
        auctionRepository.save(auction);

        return Map.of(
            "status", "success",
            "paymentId", paymentId,
            "amount", payment.getAmount()
        );
    }

    @Override
    public Map<String, Object> getPaymentStatus(Long bidId) {
        Optional<Payment> paymentOpt = paymentRepository.findFirstByBidIdOrderByPaymentTimeDesc(bidId);

        if (paymentOpt.isEmpty()) {
            throw new RuntimeException("No payment found for bidId: " + bidId);
        }

        Payment payment = paymentOpt.get();

        return Map.of(
                "bidId", bidId,
                "paymentId", payment.getPaymentId(),
                "orderId", payment.getOrderId(),
                "status", payment.getStatus(), // PENDING, PAID, etc.
                "amount", payment.getAmount(),
                "paymentTime", payment.getPaymentTime()
        );
    }

    @Override
    public Map<String, Object> getSellerPaymentStats(Long sellerId) {
        BigDecimal totalEarnings = auctionRepository.getTotalEarningsBySeller(sellerId);
        long completedAuctions = auctionRepository.countSuccessfulAuctionsBySeller(sellerId);
        long pendingPayments = paymentRepository.countPendingPaymentsForSeller(sellerId);

        return Map.of(
            "totalEarnings", totalEarnings != null ? totalEarnings : BigDecimal.ZERO,
            "completedAuctions", completedAuctions,
            "pendingPayments", pendingPayments
        );
    }
}
