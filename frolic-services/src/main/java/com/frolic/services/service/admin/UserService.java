package com.frolic.services.service.admin;

import com.frolic.core.common.dto.UserDto;
import com.frolic.core.common.exception.InvalidRequestException;
import com.frolic.core.common.exception.ResourceNotFoundException;
import com.frolic.core.repository.entity.UserEntity;
import com.frolic.core.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for user management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    
    /**
     * Get all users
     */
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Get user by ID
     */
    public UserDto getUserById(String id) {
        UserEntity entity = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return toDto(entity);
    }
    
    /**
     * Get user by email
     */
    public UserDto getUserByEmail(String email) {
        UserEntity entity = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return toDto(entity);
    }
    
    /**
     * Create a new user
     */
    @Transactional
    public UserDto createUser(UserDto dto) {
        // Check if email already exists
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new InvalidRequestException("User with email " + dto.getEmail() + " already exists");
        }
        
        UserEntity entity = new UserEntity();
        entity.setEmail(dto.getEmail());
        entity.setName(dto.getName());
        entity.setPhone(dto.getPhone());
        entity.setActive(true);
        
        entity = userRepository.save(entity);
        log.info("Created user: id={}, email={}", entity.getId(), entity.getEmail());
        
        return toDto(entity);
    }
    
    /**
     * Update existing user
     */
    @Transactional
    public UserDto updateUser(String id, UserDto dto) {
        UserEntity entity = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        
        // Check if email is being changed and if the new email already exists
        if (!entity.getEmail().equals(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new InvalidRequestException("User with email " + dto.getEmail() + " already exists");
        }
        
        entity.setEmail(dto.getEmail());
        entity.setName(dto.getName());
        entity.setPhone(dto.getPhone());
        entity.setActive(dto.isActive());
        
        entity = userRepository.save(entity);
        log.info("Updated user: id={}, email={}", entity.getId(), entity.getEmail());
        
        return toDto(entity);
    }
    
    /**
     * Delete user
     */
    @Transactional
    public void deleteUser(String id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", id);
        }
        userRepository.deleteById(id);
        log.info("Deleted user: id={}", id);
    }
    
    /**
     * Check if user exists and is active
     */
    public boolean isUserValid(String userId) {
        return userRepository.findById(userId)
            .map(UserEntity::isActive)
            .orElse(false);
    }
    
    private UserDto toDto(UserEntity entity) {
        return UserDto.builder()
            .id(entity.getId())
            .email(entity.getEmail())
            .name(entity.getName())
            .phone(entity.getPhone())
            .active(entity.isActive())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
