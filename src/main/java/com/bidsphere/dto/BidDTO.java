package com.bidsphere.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BidDTO {
    private Long id;
    
    @NotNull(message = "Auction ID is required")
    private Long auctionId;
    
    @NotNull(message = "Bidder ID is required")
    private Long bidderId;
    
    @NotNull(message = "Bid amount is required")
    @DecimalMin(value = "0.01", message = "Bid amount must be greater than 0")
    private BigDecimal amount;
    
    private LocalDateTime bidTime;
    
    // Add additional fields for frontend display
    private String auctionTitle;
    private byte[] imageData;
    private BigDecimal auctionCurrentPrice;
    private LocalDateTime auctionEndTime;
    private String status;
    private String paymentStatus;  // Will be populated from Payment entity
    private String bidderName;
}
