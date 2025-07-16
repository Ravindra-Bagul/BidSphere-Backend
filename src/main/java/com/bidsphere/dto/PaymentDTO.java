package com.bidsphere.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentDTO {
    private Long id;
    private Long bidId;
    private Long auctionId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime paymentTime;
    private String paymentId;
    private String orderId;
    private String buyerName;
    private String auctionTitle;
}
