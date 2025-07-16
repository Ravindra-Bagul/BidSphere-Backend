package com.bidsphere.controller;

import com.bidsphere.dto.AuctionDTO;
import com.bidsphere.exception.ResourceNotFoundException;
import com.bidsphere.service.AuctionService;
import com.bidsphere.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/auctions")
@CrossOrigin(origins = "http://localhost:5173")
public class AuctionController {
    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @PostMapping
    public ResponseEntity<?> createAuction(
        @RequestParam("image") MultipartFile imageFile,
        @RequestParam("title") String title,
        @RequestParam("description") String description,
        @RequestParam("category") String category,
        @RequestParam("startingPrice") Double startingPrice,
        @RequestParam("startTime") String startTime,
        @RequestParam("endTime") String endTime,
        @RequestParam("sellerId") Long sellerId
    ) {
        try {
            AuctionDTO auctionDTO = new AuctionDTO();
            auctionDTO.setTitle(title);
            auctionDTO.setDescription(description);
            auctionDTO.setCategory(category);
            auctionDTO.setStartingPrice(BigDecimal.valueOf(startingPrice));
            auctionDTO.setCurrentPrice(BigDecimal.valueOf(startingPrice));
            auctionDTO.setStartTime(LocalDateTime.parse(startTime));
            auctionDTO.setEndTime(LocalDateTime.parse(endTime));
            auctionDTO.setSellerId(sellerId);
            
            if (imageFile != null && !imageFile.isEmpty()) {
                auctionDTO.setImageData(imageFile.getBytes());
            }
            
            AuctionDTO savedAuction = auctionService.createAuction(auctionDTO);
            return ResponseEntity.ok(savedAuction);
        } catch (Exception e) {
            e.printStackTrace(); // For debugging
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Error creating auction: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<AuctionDTO>> getAllAuctions() {
        return ResponseEntity.ok(auctionService.getAllAuctions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuctionDTO> getAuction(@PathVariable Long id) {
        return ResponseEntity.ok(auctionService.getAuctionById(id));
    }

    @GetMapping("/active")
    public ResponseEntity<List<AuctionDTO>> getActiveAuctions() {
        return ResponseEntity.ok(auctionService.getActiveAuctions());
    }

    @GetMapping("/ended")
    public ResponseEntity<List<AuctionDTO>> getEndedAuctions() {
        return ResponseEntity.ok(auctionService.getEndedAuctions());
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<AuctionDTO>> getSellerAuctions(@PathVariable Long sellerId) {
        return ResponseEntity.ok(auctionService.getAuctionsBySeller(sellerId));
    }

    @GetMapping("/image/{id}")
    public ResponseEntity<?> getAuctionImage(@PathVariable Long id) {
        try {
            AuctionDTO auction = auctionService.getAuctionById(id);
            if (auction.getImageData() != null) {
                return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(auction.getImageData());
            }
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body("Image not found");
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving image: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<AuctionDTO> updateAuction(@PathVariable Long id, @RequestBody AuctionDTO auctionDTO) {
        return ResponseEntity.ok(auctionService.updateAuction(id, auctionDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAuction(@PathVariable Long id) {
        auctionService.deleteAuction(id);
        return ResponseEntity.ok().build();
    }
}
