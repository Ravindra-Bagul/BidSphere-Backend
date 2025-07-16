package com.bidsphere.service;

import com.bidsphere.dto.BidDTO;
import java.util.List;

public interface BidService {
    BidDTO placeBid(BidDTO bidDTO);
    List<BidDTO> getBidsByAuction(Long auctionId);
    List<BidDTO> getBidsByBidder(Long bidderId);
    BidDTO getHighestBid(Long auctionId);
}
