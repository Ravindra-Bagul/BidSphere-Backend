package com.bidsphere.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.time.format.DateTimeFormatter;

@Data
public class AuctionDTO {
    private Long id;
    
    @NotNull(message = "Title is required")
    private String title;
    
    private String description;
    
    @NotNull(message = "Category is required")
    private String category;
    
    @NotNull(message = "Starting price is required")
    @DecimalMin(value = "0.0")
    private BigDecimal startingPrice;
    
    private BigDecimal currentPrice;
    
    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;
    
    @NotNull(message = "End time is required")
    private LocalDateTime endTime;
    
    private String imageUrl;
    private String status;
    private Integer bidCount;
    private String sellerName;
    private Long sellerId;
    private Long winningBidId;
    private byte[] imageData;
    private String imageDataBase64;
    private BigDecimal finalPrice;
    private String buyerName;
    private String paymentStatus;

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
        if (imageData != null) {
            this.imageDataBase64 = Base64.getEncoder().encodeToString(imageData);
        }
    }
    
    public String getFormattedEndTime() {
        if (endTime == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        return endTime.format(formatter);
    }
}
