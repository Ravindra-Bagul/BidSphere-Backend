package com.bidsphere.service.impl;

import com.bidsphere.dto.BidDTO;
import com.bidsphere.entity.Auction;
import com.bidsphere.entity.AuctionStatus;
import com.bidsphere.entity.Bid;
import com.bidsphere.entity.User;
import com.bidsphere.entity.Payment;
import com.bidsphere.entity.PaymentStatus;
import com.bidsphere.exception.ResourceNotFoundException;
import com.bidsphere.repository.AuctionRepository;
import com.bidsphere.repository.BidRepository;
import com.bidsphere.repository.UserRepository;
import com.bidsphere.repository.PaymentRepository;
import com.bidsphere.service.BidService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class BidServiceImpl implements BidService {
    private static final Logger logger = LoggerFactory.getLogger(BidServiceImpl.class);

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

    public BidServiceImpl(BidRepository bidRepository, AuctionRepository auctionRepository, UserRepository userRepository, PaymentRepository paymentRepository) {
        this.bidRepository = bidRepository;
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public BidDTO placeBid(BidDTO bidDTO) {
        try {
            logger.info("Attempting to place bid: {}", bidDTO);

            if (bidDTO.getAmount() == null || bidDTO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("Invalid bid amount");
            }

            Auction auction = auctionRepository.findById(bidDTO.getAuctionId())
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));
            
            // Check if auction has started
            if (auction.getStartTime().isAfter(LocalDateTime.now())) {
                throw new IllegalStateException("Auction has not started yet");
            }

            // Check if auction has ended
            if (auction.getEndTime().isBefore(LocalDateTime.now())) {
                throw new IllegalStateException("Auction has already ended");
            }

            User bidder = userRepository.findById(bidDTO.getBidderId())
                .orElseThrow(() -> new ResourceNotFoundException("Bidder not found"));

            if (!bidder.getUserType().equals("BIDDER")) {
                throw new IllegalStateException("Only bidders can place bids");
            }

            // Validate auction status
            if (auction.getStatus() != AuctionStatus.ACTIVE) {
                throw new IllegalStateException("Auction is not active");
            }

            // Validate auction end time
            if (auction.getEndTime().isBefore(LocalDateTime.now())) {
                auction.setStatus(AuctionStatus.ENDED);
                auctionRepository.save(auction);
                throw new IllegalStateException("Auction has ended");
            }

            // Validate bid amount
            if (bidDTO.getAmount().compareTo(auction.getCurrentPrice()) <= 0) {
                throw new IllegalStateException(
                    String.format("Bid must be higher than current price: %s", 
                        auction.getCurrentPrice().toString())
                );
            }

            // Create and save bid
            Bid bid = new Bid();
            bid.setAuction(auction);
            bid.setBidder(bidder);
            bid.setAmount(bidDTO.getAmount());
            bid.setBidTime(LocalDateTime.now());

            // Update auction
            auction.setCurrentPrice(bidDTO.getAmount());
            auction.setBidCount(auction.getBidCount() + 1);
            
            auctionRepository.save(auction);
            Bid savedBid = bidRepository.save(bid);
            
            logger.info("Successfully placed bid: {}", savedBid.getId());
            
            return convertToDTO(savedBid);
        } catch (Exception e) {
            logger.error("Error placing bid: ", e);
            throw e;
        }
    }

    @Override
    public List<BidDTO> getBidsByAuction(Long auctionId) {
        try {
            logger.info("Fetching bids for auction: {}", auctionId);
            
            // First verify auction exists
            Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));

            List<Bid> bids = bidRepository.findByAuctionOrderByBidTimeDesc(auction);
            return bids.stream()
                .map(bid -> {
                    BidDTO dto = new BidDTO();
                    dto.setId(bid.getId());
                    dto.setAuctionId(bid.getAuction().getId());
                    dto.setBidderId(bid.getBidder().getId());
                    dto.setBidderName(bid.getBidder().getName());
                    dto.setAmount(bid.getAmount());
                    dto.setBidTime(bid.getBidTime());
                    return dto;
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching bids for auction: {}", auctionId, e);
            throw new RuntimeException("Error fetching bids: " + e.getMessage());
        }
    }

    @Override
    public List<BidDTO> getBidsByBidder(Long bidderId) {
        List<Bid> bids = bidRepository.findByBidderId(bidderId);
        return bids.stream().map(bid -> {
            BidDTO dto = new BidDTO();
            BeanUtils.copyProperties(bid, dto);
            
            // Set auction details
            if (bid.getAuction() != null) {
                dto.setAuctionTitle(bid.getAuction().getTitle());
                dto.setAuctionCurrentPrice(bid.getAuction().getCurrentPrice());
                dto.setAuctionEndTime(bid.getAuction().getEndTime());
                dto.setAuctionId(bid.getAuction().getId());
                dto.setImageData(bid.getAuction().getImageData());
                
                // Set bid status and payment status
                LocalDateTime now = LocalDateTime.now();
                boolean isEnded = bid.getAuction().getEndTime().isBefore(now);
                boolean isWinning = bid.getAmount().compareTo(bid.getAuction().getCurrentPrice()) >= 0;
                
                if (isEnded) {
                    if (isWinning) {
                        dto.setStatus("Won");
                        dto.setPaymentStatus(paymentRepository.findFirstByBidIdOrderByPaymentTimeDesc(bid.getId())
                            .map(payment -> payment.getStatus().toString())
                            .orElse(PaymentStatus.PENDING.toString()));
                    } else {
                        dto.setStatus("Lost");
                    }
                } else {
                    dto.setStatus(isWinning ? "Leading" : "Outbid");
                }
            }
            
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public BidDTO getHighestBid(Long auctionId) {
        try {
            logger.info("Fetching highest bid for auction: {}", auctionId);
            return bidRepository.findByAuctionIdOrderByAmountDesc(auctionId).stream()
                .findFirst()
                .map(this::convertToDTO)
                .orElse(null);
        } catch (Exception e) {
            logger.error("Error fetching highest bid for auction: {}", auctionId, e);
            throw new RuntimeException("Error fetching highest bid for auction: " + e.getMessage());
        }
    }

    private BidDTO convertToDTO(Bid bid) {
        BidDTO dto = new BidDTO();
        BeanUtils.copyProperties(bid, dto);

        // Set auction details
        Auction auction = bid.getAuction();
        if (auction != null) {
            dto.setAuctionId(auction.getId());
            dto.setAuctionTitle(auction.getTitle());
            dto.setAuctionCurrentPrice(auction.getCurrentPrice());
            dto.setAuctionEndTime(auction.getEndTime());
            dto.setImageData(auction.getImageData());
        }

        // Get payment status from Payment entity
        Optional<Payment> payment = paymentRepository.findFirstByBidIdOrderByPaymentTimeDesc(bid.getId());
        if (payment.isPresent()) {
            dto.setPaymentStatus(payment.get().getStatus().toString());
        } else {
            dto.setPaymentStatus(PaymentStatus.PENDING.toString());
        }

        return dto;
    }
}
