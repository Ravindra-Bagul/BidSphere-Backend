package com.bidsphere.service.impl;

import com.bidsphere.dto.AuctionDTO;
import com.bidsphere.entity.*;
import com.bidsphere.exception.ResourceNotFoundException;
import com.bidsphere.repository.*;
import com.bidsphere.service.AuctionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.BeanUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuctionServiceImpl implements AuctionService {
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

    public AuctionServiceImpl(AuctionRepository auctionRepository, 
                              UserRepository userRepository, 
                              PaymentRepository paymentRepository) {
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public AuctionDTO createAuction(AuctionDTO auctionDTO) {
        User seller = userRepository.findById(auctionDTO.getSellerId())
            .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

        if (!seller.getUserType().equals("SELLER")) {
            throw new IllegalStateException("Only sellers can create auctions");
        }

        Auction auction = new Auction();
        auction.setTitle(auctionDTO.getTitle());
        auction.setDescription(auctionDTO.getDescription());
        auction.setCategory(auctionDTO.getCategory());
        auction.setStartingPrice(auctionDTO.getStartingPrice());
        auction.setCurrentPrice(auctionDTO.getStartingPrice());
        auction.setStartTime(auctionDTO.getStartTime());
        auction.setEndTime(auctionDTO.getEndTime());
        auction.setImageUrl(auctionDTO.getImageUrl());
        auction.setImageData(auctionDTO.getImageData());
        auction.setSeller(seller);
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setBidCount(0);

        Auction savedAuction = auctionRepository.save(auction);
        return convertToDTO(savedAuction);
    }

    @Override
    public AuctionDTO getAuctionById(Long id) {
        Auction auction = auctionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));
        return convertToDTO(auction);
    }

    @Override
    @Transactional
    public List<AuctionDTO> getAllAuctions() {
        List<Auction> auctions = auctionRepository.findAll();
        return auctions.stream().map(auction -> {
            AuctionDTO dto = mapToDTO(auction);
            if (auction.getWinningBid() != null) {
                Payment payment = paymentRepository
                    .findFirstByBidIdOrderByPaymentTimeDesc(auction.getWinningBid().getId())
                    .orElse(null);
                dto.setPaymentStatus(payment != null ? payment.getStatus().toString() : "PENDING");
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<AuctionDTO> getAuctionsBySeller(Long sellerId) {
        // First update status of ended auctions
        updateAuctionStatuses();
        
        List<Auction> auctions = auctionRepository.findBySellerId(sellerId);
        
        // Filter and process auctions
        List<AuctionDTO> dtos = auctions.stream()
            .peek(auction -> {
                // Update status if auction has ended
                if (auction.getEndTime().isBefore(LocalDateTime.now()) && 
                    auction.getStatus() == AuctionStatus.ACTIVE) {
                    auction.setStatus(AuctionStatus.ENDED);
                    auctionRepository.save(auction);
                }
            })
            .map(this::convertToDTO)
            .collect(Collectors.toList());
            
        return dtos;
    }

    @Override
    @Transactional
    public Map<String, Object> getSellerStats(Long sellerId) {
        // Update auction statuses first
        auctionRepository.updateEndedAuctionStatuses();
        
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Get only currently active auctions
        long activeAuctions = auctionRepository.countBySellerIdAndStatusAndEndTimeAfter(
            sellerId, 
            AuctionStatus.ACTIVE,
            now
        );
        
        long totalAuctions = auctionRepository.countTotalAuctionsBySeller(sellerId);
        BigDecimal totalEarnings = auctionRepository.getTotalEarningsBySeller(sellerId);
        
        stats.put("activeAuctions", activeAuctions);
        stats.put("totalAuctions", totalAuctions);
        stats.put("totalEarnings", totalEarnings != null ? totalEarnings : BigDecimal.ZERO);
        
        return stats;
    }

    @Override
    public List<AuctionDTO> getActiveAuctions() {
        return auctionRepository.findByStatusOrderByEndTimeAsc(AuctionStatus.ACTIVE)
            .stream()
            .map(auction -> {
                AuctionDTO dto = convertToDTO(auction);
                dto.setBidCount(auction.getBidCount());
                dto.setSellerName(auction.getSeller().getName());
                return dto;
            })
            .collect(Collectors.toList());
    }

    @Override
    public AuctionDTO updateAuction(Long id, AuctionDTO auctionDTO) {
        Auction auction = auctionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));
        
        BeanUtils.copyProperties(auctionDTO, auction, "id", "seller", "status", "bidCount");
        Auction updatedAuction = auctionRepository.save(auction);
        return convertToDTO(updatedAuction);
    }

    @Override
    public void deleteAuction(Long id) {
        if (!auctionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Auction not found");
        }
        auctionRepository.deleteById(id);
    }

    @Override
    public List<AuctionDTO> getEndedAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> endedAuctions = auctionRepository.findEndedAuctions(now);
        
        return endedAuctions.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    private void updateAuctionStatuses() {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> activeAuctions = auctionRepository.findByStatusOrderByEndTimeAsc(AuctionStatus.ACTIVE);
        
        activeAuctions.stream()
            .filter(auction -> auction.getEndTime().isBefore(now))
            .forEach(auction -> {
                auction.setStatus(AuctionStatus.ENDED);
                auctionRepository.save(auction);
            });
    }

    private AuctionDTO convertToDTO(Auction auction) {
        AuctionDTO dto = new AuctionDTO();
        BeanUtils.copyProperties(auction, dto);
        
        // Get the highest bid for this auction
        Optional<Bid> highestBid = auction.getBids().stream()
            .max(Comparator.comparing(Bid::getAmount));
            
        // Set final price and winner for ended auctions
        if (auction.getStatus() == AuctionStatus.ENDED || 
            auction.getEndTime().isBefore(LocalDateTime.now())) {
            if (highestBid.isPresent()) {
                dto.setFinalPrice(highestBid.get().getAmount());
                dto.setBuyerName(highestBid.get().getBidder().getName());
                dto.setStatus("ENDED");
                
                // Get payment status if exists
                Payment payment = paymentRepository.findFirstByBidIdOrderByPaymentTimeDesc(highestBid.get().getId()).orElse(null);
                if (payment != null) {
                    dto.setPaymentStatus(payment.getStatus().toString());
                } else {
                    dto.setPaymentStatus("PENDING");
                }
            } else {
                dto.setFinalPrice(auction.getStartingPrice());
                dto.setBuyerName("No Bidder");
                dto.setStatus("ENDED");
            }
        }
        
        if (auction.getSeller() != null) {
            dto.setSellerId(auction.getSeller().getId());
            dto.setSellerName(auction.getSeller().getName());
        }
        
        dto.setBidCount(auction.getBidCount());
        dto.setStatus(auction.getStatus().toString());
        
        // Set image data
        if (auction.getImageData() != null) {
            dto.setImageData(auction.getImageData());
        }

        // Set buyer info and payment status if auction has winning bid
        if (auction.getWinningBid() != null) {  // Fix: Use getter method instead of direct field access
            Bid winningBid = auction.getWinningBid();
            dto.setBuyerName(winningBid.getBidder().getName());
            dto.setWinningBidId(winningBid.getId());

            // Get payment status
            Payment payment = paymentRepository.findFirstByBidIdOrderByPaymentTimeDesc(winningBid.getId()).orElse(null);
            if (payment != null) {
                dto.setPaymentStatus(payment.getStatus().toString());
            } else {
                dto.setPaymentStatus("PENDING");
            }
        }
        
        return dto;
    }

    private AuctionDTO mapToDTO(Auction auction) {
        AuctionDTO dto = new AuctionDTO();
        BeanUtils.copyProperties(auction, dto);
        
        // Get the highest bid for this auction
        Optional<Bid> highestBid = auction.getBids().stream()
            .max(Comparator.comparing(Bid::getAmount));
            
        // Set final price and winner for ended auctions
        if (auction.getStatus() == AuctionStatus.ENDED || 
            auction.getEndTime().isBefore(LocalDateTime.now())) {
            if (highestBid.isPresent()) {
                dto.setFinalPrice(highestBid.get().getAmount());
                dto.setBuyerName(highestBid.get().getBidder().getName());
                dto.setStatus("ENDED");
                
                // Get payment status if exists
                Payment payment = paymentRepository.findFirstByBidIdOrderByPaymentTimeDesc(highestBid.get().getId()).orElse(null);
                if (payment != null) {
                    dto.setPaymentStatus(payment.getStatus().toString());
                } else {
                    dto.setPaymentStatus("PENDING");
                }
            } else {
                dto.setFinalPrice(auction.getStartingPrice());
                dto.setBuyerName("No Bidder");
                dto.setStatus("ENDED");
            }
        }
        
        if (auction.getSeller() != null) {
            dto.setSellerId(auction.getSeller().getId());
            dto.setSellerName(auction.getSeller().getName());
        }
        
        dto.setBidCount(auction.getBidCount());
        dto.setStatus(auction.getStatus().toString());
        
        // Set image data
        if (auction.getImageData() != null) {
            dto.setImageData(auction.getImageData());
        }

        // Set buyer info and payment status if auction has winning bid
        if (auction.getWinningBid() != null) {  // Fix: Use getter method instead of direct field access
            Bid winningBid = auction.getWinningBid();
            dto.setBuyerName(winningBid.getBidder().getName());
            dto.setWinningBidId(winningBid.getId());

            // Get payment status
            Payment payment = paymentRepository.findFirstByBidIdOrderByPaymentTimeDesc(winningBid.getId()).orElse(null);
            if (payment != null) {
                dto.setPaymentStatus(payment.getStatus().toString());
            } else {
                dto.setPaymentStatus("PENDING");
            }
        }
        
        return dto;
    }
}
