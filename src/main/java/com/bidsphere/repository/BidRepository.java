package com.bidsphere.repository;

import com.bidsphere.entity.Bid;
import com.bidsphere.entity.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByAuctionIdOrderByAmountDesc(Long auctionId);
    List<Bid> findByBidderId(Long bidderId);
    List<Bid> findByAuctionOrderByBidTimeDesc(Auction auction);
    List<Bid> findByAuctionIdOrderByBidTimeDesc(Long auctionId);

    @Query("SELECT b FROM Bid b JOIN b.auction a WHERE b.bidder.id = :bidderId AND a.status = 'ENDED' AND b.amount = (SELECT MAX(b2.amount) FROM Bid b2 WHERE b2.auction = a)")
    List<Bid> findWinningBidsByBidder(@Param("bidderId") Long bidderId);

    @Query("SELECT SUM(b.amount) FROM Bid b WHERE b.bidder.id = :bidderId AND b.auction.status = 'ENDED' AND b = b.auction.winningBid")
    BigDecimal getTotalSpentByBidder(@Param("bidderId") Long bidderId);
}
