package com.bidsphere.entity;

import jakarta.persistence.*;
import lombok.Data;
import javax.sql.rowset.serial.SerialBlob;
import java.sql.Blob;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "auctions")
public class Auction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(length = 1000)
    private String description;
    
    @Column(nullable = false)
    private String category;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal startingPrice;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal currentPrice;
    
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;
    
    private String imageUrl;
    
    @Lob
    @Column(name = "image_data", columnDefinition="LONGBLOB")
    private byte[] imageData;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;
    
    @Column(nullable = false)
    private Integer bidCount = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status = AuctionStatus.ACTIVE;
    
    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL)
    private List<Bid> bids = new ArrayList<>();
    
    @OneToOne
    @JoinColumn(name = "winning_bid_id")
    private Bid winningBid;
}
