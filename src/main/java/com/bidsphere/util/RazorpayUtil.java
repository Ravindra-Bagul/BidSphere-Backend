package com.bidsphere.util;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import jakarta.annotation.PostConstruct;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.HashMap;

@Component
public class RazorpayUtil {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    private RazorpayClient razorpayClient;

    @PostConstruct
    public void init() {
        try {
            this.razorpayClient = new RazorpayClient(keyId, keySecret);
            System.out.println("Razorpay client initialized with key ID: " + keyId);
        } catch (RazorpayException e) {
            throw new RuntimeException("Failed to initialize Razorpay client", e);
        }
    }

    public Map<String, Object> createOrder(long amount, String currency, String receipt) throws RazorpayException {
        try {
            System.out.println("Creating order with amount: " + amount);
            
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount);
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", receipt);
            orderRequest.put("payment_capture", 1);
            
            System.out.println("Order request: " + orderRequest);
            
            Order order = razorpayClient.orders.create(orderRequest);
            System.out.println("Order created: " + order);
            
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.get("id"));
            response.put("amount", order.get("amount"));
            response.put("currency", order.get("currency"));
            response.put("key", keyId);
            
            return response;
        } catch (RazorpayException e) {
            System.err.println("Razorpay error: " + e.getMessage());
            throw e;
        }
    }

    public boolean verifySignature(String orderId, String paymentId, String razorpaySignature) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", razorpaySignature);
            
            return Utils.verifyPaymentSignature(attributes, keySecret);
        } catch (Exception e) {
            System.err.println("Signature verification error: " + e.getMessage());
            return false;
        }
    }
}
