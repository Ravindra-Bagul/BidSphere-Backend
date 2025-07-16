package com.bidsphere.repository;

import com.bidsphere.entity.Auction;
import com.bidsphere.entity.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {
    List<Auction> findByStatusOrderByEndTimeAsc(AuctionStatus status);
    List<Auction> findBySellerIdAndStatusOrderByStartTimeDesc(Long sellerId, AuctionStatus status);
    
    @Query("SELECT a FROM Auction a WHERE a.seller.id = :sellerId")
    List<Auction> findBySellerId(@Param("sellerId") Long sellerId);
    
    @Query("SELECT a FROM Auction a WHERE a.seller.id = :sellerId AND a.status = :status")
    List<Auction> findBySellerIdAndStatus(@Param("sellerId") Long sellerId, @Param("status") AuctionStatus status);
    
    @Query("SELECT a FROM Auction a WHERE a.status = :status AND a.endTime > :now")
    List<Auction> findActiveAuctions(@Param("status") AuctionStatus status, @Param("now") LocalDateTime now);
    
    @Query("SELECT a FROM Auction a WHERE a.seller.id = :sellerId AND " +
           "((a.status = :status AND a.endTime > CURRENT_TIMESTAMP) OR " +
           "(a.status = 'ENDED' AND a.endTime <= CURRENT_TIMESTAMP)) " +
           "ORDER BY a.startTime DESC")
    List<Auction> findActiveAndEndedBySeller(@Param("sellerId") Long sellerId, 
                                           @Param("status") AuctionStatus status);

    @Query("SELECT COUNT(a) FROM Auction a WHERE a.seller.id = :sellerId")
    long countBySellerId(@Param("sellerId") Long sellerId);

    @Query("SELECT COUNT(a) FROM Auction a WHERE a.seller.id = :sellerId AND a.status = :status")
    long countBySellerIdAndStatus(@Param("sellerId") Long sellerId, @Param("status") AuctionStatus status);

    @Query("SELECT COUNT(a) FROM Auction a WHERE a.seller.id = :sellerId AND a.status = :status AND a.endTime > :now")
    long countBySellerIdAndStatusAndEndTimeAfter(
        @Param("sellerId") Long sellerId, 
        @Param("status") AuctionStatus status,
        @Param("now") LocalDateTime now
    );

    @Query("SELECT SUM(a.winningBid.amount) FROM Auction a WHERE a.seller.id = :sellerId AND a.status = 'ENDED' AND a.winningBid IS NOT NULL")
    BigDecimal getTotalEarningsBySeller(@Param("sellerId") Long sellerId);

    @Query("SELECT COUNT(a) FROM Auction a WHERE a.seller.id = :sellerId AND a.status = 'ENDED' AND a.winningBid IS NOT NULL")
    long countSuccessfulAuctionsBySeller(@Param("sellerId") Long sellerId);

    @Query("SELECT COUNT(DISTINCT a) FROM Auction a WHERE a.seller.id = :sellerId")
    long countTotalAuctionsBySeller(@Param("sellerId") Long sellerId);

    @Modifying
    @Query("UPDATE Auction a SET a.status = 'ENDED' WHERE a.status = 'ACTIVE' AND a.endTime < CURRENT_TIMESTAMP")
    void updateEndedAuctionStatuses();

    @Query("SELECT a FROM Auction a WHERE a.seller.id = :sellerId AND ((a.status = 'ACTIVE' AND a.endTime > CURRENT_TIMESTAMP) OR a.status = 'ENDED')")
    List<Auction> findAllAuctionsWithCorrectStatus(@Param("sellerId") Long sellerId);

    @Query("SELECT a FROM Auction a WHERE a.status = 'ENDED' OR a.endTime <= :now")
    List<Auction> findEndedAuctions(@Param("now") LocalDateTime now);
}
