package com.bidsphere.service;

import com.bidsphere.dto.UserDTO;
import java.util.List;
import java.util.Map;

public interface UserService {
    UserDTO createUser(UserDTO userDTO);
    UserDTO getUserById(Long id);
    List<UserDTO> getAllUsers();
    UserDTO updateUser(Long id, UserDTO userDTO);
    void deleteUser(Long id);
    UserDTO findByEmail(String email);
    Map<String, Object> getBidderStats(Long bidderId);
    Map<String, Object> getSellerStats(Long sellerId);
}
