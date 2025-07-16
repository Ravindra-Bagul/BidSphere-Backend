package com.bidsphere.service;

import com.bidsphere.dto.AuctionDTO;
import java.util.List;
import java.util.Map;

public interface AuctionService {
    AuctionDTO createAuction(AuctionDTO auctionDTO);
    AuctionDTO getAuctionById(Long id);
    List<AuctionDTO> getAllAuctions();
    List<AuctionDTO> getAuctionsBySeller(Long sellerId);
    List<AuctionDTO> getActiveAuctions();
    List<AuctionDTO> getEndedAuctions(); // Add this method
    AuctionDTO updateAuction(Long id, AuctionDTO auctionDTO);
    void deleteAuction(Long id);
    Map<String, Object> getSellerStats(Long sellerId);
}
