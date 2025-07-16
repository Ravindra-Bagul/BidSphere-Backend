package com.bidsphere.service.impl;

import com.bidsphere.dto.UserDTO;
import com.bidsphere.entity.User;
import com.bidsphere.entity.Bid;
import com.bidsphere.entity.Auction;
import com.bidsphere.entity.AuctionStatus;
import com.bidsphere.repository.UserRepository;
import com.bidsphere.repository.BidRepository;
import com.bidsphere.repository.AuctionRepository;
import com.bidsphere.repository.PaymentRepository;
import com.bidsphere.service.UserService;
import com.bidsphere.exception.ResourceNotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final PaymentRepository paymentRepository;

    public UserServiceImpl(UserRepository userRepository, 
                           PasswordEncoder passwordEncoder,
                           BidRepository bidRepository,
                           AuctionRepository auctionRepository,
                           PaymentRepository paymentRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bidRepository = bidRepository;
        this.auctionRepository = auctionRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional
    public UserDTO createUser(UserDTO userDTO) {
        try {
            // Validate email uniqueness
            if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
                throw new IllegalStateException("Email already exists");
            }

            // Validate and normalize userType
            String userType = userDTO.getUserType().toUpperCase();
            if (!userType.equals("SELLER") && !userType.equals("BIDDER")) {
                throw new IllegalStateException("Invalid user type. Must be SELLER or BIDDER");
            }
            userDTO.setUserType(userType);

            // Create new user
            User user = new User();
            user.setName(userDTO.getName());
            user.setEmail(userDTO.getEmail());
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
            user.setUserType(userDTO.getUserType());
            
            // Save user
            User savedUser = userRepository.save(user);
            
            // Prepare response
            UserDTO responseDTO = new UserDTO();
            responseDTO.setId(savedUser.getId());
            responseDTO.setName(savedUser.getName());
            responseDTO.setEmail(savedUser.getEmail());
            responseDTO.setUserType(savedUser.getUserType());
            // Don't set password in response
            
            return responseDTO;
        } catch (Exception e) {
            throw new IllegalStateException("Error creating user: " + e.getMessage());
        }
    }

    @Override
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        return userDTO;
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
            .map(user -> {
                UserDTO dto = new UserDTO();
                BeanUtils.copyProperties(user, dto);
                return dto;
            })
            .collect(Collectors.toList());
    }

    @Override
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        BeanUtils.copyProperties(userDTO, user, "id");
        User updatedUser = userRepository.save(user);
        BeanUtils.copyProperties(updatedUser, userDTO);
        return userDTO;
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found");
        }
        userRepository.deleteById(id);
    }

    @Override
    public UserDTO findByEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        return userDTO;
    }

    @Override
    @Transactional
    public Map<String, Object> getBidderStats(Long bidderId) {
        User bidder = userRepository.findById(bidderId)
            .orElseThrow(() -> new ResourceNotFoundException("Bidder not found"));

        // Update auction statuses first
        auctionRepository.updateEndedAuctionStatuses();

        // Get all bids and winning bids
        List<Bid> allBids = bidRepository.findByBidderId(bidderId);
        List<Bid> winningBids = bidRepository.findWinningBidsByBidder(bidderId);

        // Calculate total spent from winning bids
        BigDecimal totalSpent = winningBids.stream()
            .map(Bid::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new HashMap<>();
        stats.put("name", bidder.getName());
        stats.put("email", bidder.getEmail());
        stats.put("totalBids", allBids.size());
        stats.put("wonAuctions", winningBids.size());
        stats.put("totalSpent", totalSpent);

        return stats;
    }

    @Override
    @Transactional
    public Map<String, Object> getSellerStats(Long sellerId) {
        User seller = userRepository.findById(sellerId)
            .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

        // First, update any auctions that should be marked as ended
        auctionRepository.updateEndedAuctionStatuses();

        // Get total auctions
        long totalAuctions = auctionRepository.countBySellerId(sellerId);

        // Get only currently active auctions
        LocalDateTime now = LocalDateTime.now();
        long activeAuctions = auctionRepository.countBySellerIdAndStatusAndEndTimeAfter(
            sellerId, 
            AuctionStatus.ACTIVE,
            now
        );

        // Get total earnings from completed auctions
        BigDecimal totalEarnings = Optional.ofNullable(
            auctionRepository.getTotalEarningsBySeller(sellerId)
        ).orElse(BigDecimal.ZERO);

        Map<String, Object> stats = new HashMap<>();
        stats.put("name", seller.getName());
        stats.put("email", seller.getEmail());
        stats.put("totalAuctions", totalAuctions);
        stats.put("activeAuctions", activeAuctions);
        stats.put("totalEarnings", totalEarnings);

        return stats;
    }
}
