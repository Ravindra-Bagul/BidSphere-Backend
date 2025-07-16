package com.bidsphere.controller;

import com.bidsphere.dto.BidDTO;
import com.bidsphere.exception.ResourceNotFoundException;
import com.bidsphere.service.BidService;
import com.bidsphere.dto.ErrorResponse;
import com.bidsphere.entity.Auction;
import com.bidsphere.repository.AuctionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/bids")
@CrossOrigin(origins = "http://localhost:5173")
public class BidController {
    private final BidService bidService;
    private final AuctionRepository auctionRepository;

    public BidController(BidService bidService, AuctionRepository auctionRepository) {
        this.bidService = bidService;
        this.auctionRepository = auctionRepository;
    }

    @PostMapping
    public ResponseEntity<?> placeBid(@Valid @RequestBody BidDTO bidDTO) {
        try {
            BidDTO savedBid = bidService.placeBid(bidDTO);
            return ResponseEntity.ok(savedBid);
        } catch (IllegalStateException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, e.getMessage()));
        }
    }

    @GetMapping("/auction/{auctionId}")
    public ResponseEntity<?> getAuctionBids(@PathVariable Long auctionId) {
        try {
            // First check if auction exists
            Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));

            List<BidDTO> bids = bidService.getBidsByAuction(auctionId);
            return ResponseEntity.ok(bids);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace(); // for debugging
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Error fetching bids: " + e.getMessage()));
        }
    }

    @GetMapping("/bidder/{bidderId}")
    public ResponseEntity<?> getBidderBids(@PathVariable Long bidderId) {
        try {
            List<BidDTO> bids = bidService.getBidsByBidder(bidderId);
            return ResponseEntity.ok(bids);
        } catch (Exception e) {
            e.printStackTrace(); // Add debug logging
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, e.getMessage()));
        }
    }

    @GetMapping("/auction/{auctionId}/highest")
    public ResponseEntity<BidDTO> getHighestBid(@PathVariable Long auctionId) {
        BidDTO highestBid = bidService.getHighestBid(auctionId);
        return highestBid != null ? ResponseEntity.ok(highestBid) : ResponseEntity.notFound().build();
    }
}
