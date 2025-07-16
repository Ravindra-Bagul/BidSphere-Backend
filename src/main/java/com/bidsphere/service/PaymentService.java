package com.bidsphere.service;

import java.util.Map;

public interface PaymentService {

    Map<String, Object> createPaymentOrder(Long bidId);
    Map<String, Object> verifyAndUpdatePayment(Long bidId, String orderId, String paymentId, String signature);
    Map<String, Object> getSellerPaymentStats(Long sellerId);
    Map<String, Object> getPaymentStatus(Long bidId);

}
