package com.frolic.services.service.admin;

import com.frolic.core.common.dto.UserDto;
import com.frolic.core.common.exception.InvalidRequestException;
import com.frolic.core.common.exception.ResourceNotFoundException;
import com.frolic.core.repository.entity.UserEntity;
import com.frolic.core.repository.jpa.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void testGetAllUsers_ReturnsAllUsers() {
        UserEntity user1 = createUserEntity("user-1", "user1@test.com", "User 1");
        UserEntity user2 = createUserEntity("user-2", "user2@test.com", "User 2");
        
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));
        
        List<UserDto> result = userService.getAllUsers();
        
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("user-1");
        assertThat(result.get(1).getId()).isEqualTo("user-2");
        
        verify(userRepository).findAll();
    }
    
    @Test
    void testGetUserById_ValidId_ReturnsUser() {
        UserEntity user = createUserEntity("user-1", "user1@test.com", "User 1");
        
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        
        UserDto result = userService.getUserById("user-1");
        
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("user-1");
        assertThat(result.getEmail()).isEqualTo("user1@test.com");
        assertThat(result.getName()).isEqualTo("User 1");
        
        verify(userRepository).findById("user-1");
    }
    
    @Test
    void testGetUserById_InvalidId_ThrowsResourceNotFoundException() {
        when(userRepository.findById("invalid-id")).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> userService.getUserById("invalid-id"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User")
            .hasMessageContaining("invalid-id");
        
        verify(userRepository).findById("invalid-id");
    }
    
    @Test
    void testGetUserByEmail_ValidEmail_ReturnsUser() {
        UserEntity user = createUserEntity("user-1", "user1@test.com", "User 1");
        
        when(userRepository.findByEmail("user1@test.com")).thenReturn(Optional.of(user));
        
        UserDto result = userService.getUserByEmail("user1@test.com");
        
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("user1@test.com");
        
        verify(userRepository).findByEmail("user1@test.com");
    }
    
    @Test
    void testGetUserByEmail_InvalidEmail_ThrowsResourceNotFoundException() {
        when(userRepository.findByEmail("invalid@test.com")).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> userService.getUserByEmail("invalid@test.com"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User")
            .hasMessageContaining("invalid@test.com");
        
        verify(userRepository).findByEmail("invalid@test.com");
    }
    
    @Test
    void testCreateUser_ValidDto_CreatesUser() {
        UserDto dto = UserDto.builder()
            .email("newuser@test.com")
            .name("New User")
            .phone("+1234567890")
            .build();
        
        UserEntity savedEntity = createUserEntity("user-1", "newuser@test.com", "New User");
        
        when(userRepository.existsByEmail("newuser@test.com")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedEntity);
        
        UserDto result = userService.createUser(dto);
        
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("user-1");
        assertThat(result.getEmail()).isEqualTo("newuser@test.com");
        assertThat(result.isActive()).isTrue();
        
        verify(userRepository).existsByEmail("newuser@test.com");
        verify(userRepository).save(any(UserEntity.class));
    }
    
    @Test
    void testCreateUser_EmailAlreadyExists_ThrowsInvalidRequestException() {
        UserDto dto = UserDto.builder()
            .email("existing@test.com")
            .name("New User")
            .build();
        
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);
        
        assertThatThrownBy(() -> userService.createUser(dto))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("User with email existing@test.com already exists");
        
        verify(userRepository).existsByEmail("existing@test.com");
        verify(userRepository, never()).save(any(UserEntity.class));
    }
    
    @Test
    void testUpdateUser_ValidIdAndDto_UpdatesUser() {
        UserEntity existingEntity = createUserEntity("user-1", "old@test.com", "Old User");
        
        when(userRepository.findById("user-1")).thenReturn(Optional.of(existingEntity));
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        UserDto dto = UserDto.builder()
            .email("new@test.com")
            .name("Updated User")
            .phone("+9876543210")
            .active(false)
            .build();
        
        UserDto result = userService.updateUser("user-1", dto);
        
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("new@test.com");
        assertThat(result.getName()).isEqualTo("Updated User");
        assertThat(result.isActive()).isFalse();
        
        verify(userRepository).findById("user-1");
        verify(userRepository).existsByEmail("new@test.com");
        verify(userRepository).save(any(UserEntity.class));
    }
    
    @Test
    void testUpdateUser_EmailAlreadyExists_ThrowsInvalidRequestException() {
        UserEntity existingEntity = createUserEntity("user-1", "old@test.com", "Old User");
        
        when(userRepository.findById("user-1")).thenReturn(Optional.of(existingEntity));
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);
        
        UserDto dto = UserDto.builder()
            .email("existing@test.com")
            .name("Updated User")
            .build();
        
        assertThatThrownBy(() -> userService.updateUser("user-1", dto))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("User with email existing@test.com already exists");
        
        verify(userRepository).findById("user-1");
        verify(userRepository).existsByEmail("existing@test.com");
        verify(userRepository, never()).save(any(UserEntity.class));
    }
    
    @Test
    void testUpdateUser_InvalidId_ThrowsResourceNotFoundException() {
        when(userRepository.findById("invalid-id")).thenReturn(Optional.empty());
        
        UserDto dto = UserDto.builder()
            .email("updated@test.com")
            .name("Updated User")
            .build();
        
        assertThatThrownBy(() -> userService.updateUser("invalid-id", dto))
            .isInstanceOf(ResourceNotFoundException.class);
        
        verify(userRepository).findById("invalid-id");
        verify(userRepository, never()).save(any(UserEntity.class));
    }
    
    @Test
    void testDeleteUser_ValidId_DeletesUser() {
        when(userRepository.existsById("user-1")).thenReturn(true);
        doNothing().when(userRepository).deleteById("user-1");
        
        assertThatCode(() -> userService.deleteUser("user-1"))
            .doesNotThrowAnyException();
        
        verify(userRepository).existsById("user-1");
        verify(userRepository).deleteById("user-1");
    }
    
    @Test
    void testDeleteUser_InvalidId_ThrowsResourceNotFoundException() {
        when(userRepository.existsById("invalid-id")).thenReturn(false);
        
        assertThatThrownBy(() -> userService.deleteUser("invalid-id"))
            .isInstanceOf(ResourceNotFoundException.class);
        
        verify(userRepository).existsById("invalid-id");
        verify(userRepository, never()).deleteById(any());
    }
    
    @Test
    void testIsUserValid_ValidAndActiveUser_ReturnsTrue() {
        UserEntity user = createUserEntity("user-1", "user@test.com", "User");
        user.setActive(true);
        
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        
        boolean result = userService.isUserValid("user-1");
        
        assertThat(result).isTrue();
        verify(userRepository).findById("user-1");
    }
    
    @Test
    void testIsUserValid_InactiveUser_ReturnsFalse() {
        UserEntity user = createUserEntity("user-1", "user@test.com", "User");
        user.setActive(false);
        
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        
        boolean result = userService.isUserValid("user-1");
        
        assertThat(result).isFalse();
        verify(userRepository).findById("user-1");
    }
    
    @Test
    void testIsUserValid_UserNotFound_ReturnsFalse() {
        when(userRepository.findById("invalid-id")).thenReturn(Optional.empty());
        
        boolean result = userService.isUserValid("invalid-id");
        
        assertThat(result).isFalse();
        verify(userRepository).findById("invalid-id");
    }
    
    private UserEntity createUserEntity(String id, String email, String name) {
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setEmail(email);
        entity.setName(name);
        entity.setPhone("+1234567890");
        entity.setActive(true);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }
}
