package com.bidsphere.controller;

import com.bidsphere.dto.ErrorResponse;
import com.bidsphere.service.PaymentService;
import com.bidsphere.util.RazorpayUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:5173")
public class PaymentController {
    private final PaymentService paymentService;
    private final RazorpayUtil razorpayUtil;

    public PaymentController(PaymentService paymentService, RazorpayUtil razorpayUtil) {
        this.paymentService = paymentService;
        this.razorpayUtil = razorpayUtil;
    }

    @GetMapping("/seller/{sellerId}/stats")
    public ResponseEntity<Map<String, Object>> getSellerPaymentStats(@PathVariable Long sellerId) {
        return ResponseEntity.ok(paymentService.getSellerPaymentStats(sellerId));
    }

    @GetMapping("/status/{bidId}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable Long bidId) {
        try {
            Map<String, Object> paymentStatus = paymentService.getPaymentStatus(bidId);
            return ResponseEntity.ok(paymentStatus);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(500, "Failed to fetch payment status: " + e.getMessage()));
        }
    }

    @PostMapping("/update/{bidId}")
    public ResponseEntity<?> updatePaymentStatus(
        @PathVariable Long bidId,
        @RequestBody Map<String, Object> paymentDetails
    ) {
        try {
            String orderId = (String) paymentDetails.get("orderId");
            String paymentId = (String) paymentDetails.get("paymentId");
            String signature = (String) paymentDetails.get("signature");
            
            // Verify signature
            if (!razorpayUtil.verifySignature(orderId, paymentId, signature)) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse(400, "Invalid payment signature"));
            }
            
            Map<String, Object> response = paymentService.verifyAndUpdatePayment(
                bidId,
                orderId,
                paymentId,
                signature
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Failed to update payment status: " + e.getMessage()));
        }
    }

    @PostMapping("/create-order/{bidId}")
    public ResponseEntity<?> createOrder(@PathVariable Long bidId) {
        try {
            Map<String, Object> order = paymentService.createPaymentOrder(bidId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, e.getMessage()));
        }
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("Received order request: " + request); // Debug log
            
            // Extract bid ID and validate
            Long bidId = Long.parseLong(String.valueOf(request.get("bidId")));
            if (bidId == null) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse(400, "Bid ID is required"));
            }

            // Create payment order
            Map<String, Object> orderData = paymentService.createPaymentOrder(bidId);
            
            System.out.println("Created order: " + orderData); // Debug log
            return ResponseEntity.ok(orderData);
            
        } catch (Exception e) {
            e.printStackTrace(); // Debug log
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Failed to create payment order: " + e.getMessage()));
        }
    }

    @PostMapping("/verify/{bidId}")
    public ResponseEntity<?> verifyPayment(
        @PathVariable Long bidId,
        @RequestBody Map<String, String> paymentDetails
    ) {
        try {
            Map<String, Object> result = paymentService.verifyAndUpdatePayment(
                bidId,
                paymentDetails.get("orderId"),
                paymentDetails.get("paymentId"),
                paymentDetails.get("signature")
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> paymentDetails) {
        try {
            System.out.println("Verifying payment: " + paymentDetails); // Debug log

            Long bidId = Long.parseLong(paymentDetails.get("bidId"));
            String orderId = paymentDetails.get("orderId");
            String paymentId = paymentDetails.get("paymentId");
            String signature = paymentDetails.get("signature");

            Map<String, Object> result = paymentService.verifyAndUpdatePayment(
                bidId,
                orderId,
                paymentId,
                signature
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace(); // Debug log
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Payment verification failed: " + e.getMessage()));
        }
    }
}
