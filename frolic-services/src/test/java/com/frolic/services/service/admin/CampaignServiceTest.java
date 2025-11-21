package com.frolic.services.service.admin;

import com.frolic.core.common.dto.CampaignDto;
import com.frolic.core.common.enums.CampaignStatus;
import com.frolic.core.common.enums.GameStatus;
import com.frolic.core.common.exception.ResourceNotFoundException;
import com.frolic.core.repository.entity.CampaignEntity;
import com.frolic.core.repository.entity.GameEntity;
import com.frolic.core.repository.jpa.CampaignRepository;
import com.frolic.core.repository.jpa.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CampaignService
 */
@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {
    
    @Mock
    private CampaignRepository campaignRepository;
    
    @Mock
    private GameRepository gameRepository;
    
    @Mock
    private GameService gameService;
    
    private CampaignService campaignService;
    
    @BeforeEach
    void setUp() {
        campaignService = new CampaignService(campaignRepository, gameRepository, gameService);
    }
    
    @Test
    void testGetAllCampaigns_ReturnsAllCampaigns() {
        CampaignEntity campaign1 = createCampaignEntity("1", "Campaign 1", CampaignStatus.ACTIVE);
        CampaignEntity campaign2 = createCampaignEntity("2", "Campaign 2", CampaignStatus.DRAFT);
        
        when(campaignRepository.findAll()).thenReturn(Arrays.asList(campaign1, campaign2));
        
        List<CampaignDto> result = campaignService.getAllCampaigns();
        
        assertEquals(2, result.size());
        assertEquals("Campaign 1", result.get(0).getName());
        assertEquals("Campaign 2", result.get(1).getName());
    }
    
    @Test
    void testGetAllCampaigns_EmptyList() {
        when(campaignRepository.findAll()).thenReturn(Collections.emptyList());
        
        List<CampaignDto> result = campaignService.getAllCampaigns();
        
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testGetCampaignById_Success() {
        CampaignEntity entity = createCampaignEntity("123", "Test Campaign", CampaignStatus.ACTIVE);
        
        when(campaignRepository.findById("123")).thenReturn(Optional.of(entity));
        
        CampaignDto result = campaignService.getCampaignById("123");
        
        assertNotNull(result);
        assertEquals("123", result.getId());
        assertEquals("Test Campaign", result.getName());
        assertEquals(CampaignStatus.ACTIVE, result.getStatus());
    }
    
    @Test
    void testGetCampaignById_NotFound_ThrowsException() {
        when(campaignRepository.findById("nonexistent")).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> 
            campaignService.getCampaignById("nonexistent")
        );
    }
    
    @Test
    void testGetCampaignsByStatus_ReturnsMatchingCampaigns() {
        CampaignEntity campaign1 = createCampaignEntity("1", "Active 1", CampaignStatus.ACTIVE);
        CampaignEntity campaign2 = createCampaignEntity("2", "Active 2", CampaignStatus.ACTIVE);
        
        when(campaignRepository.findByStatusOrderByCreatedAtDesc(CampaignStatus.ACTIVE))
            .thenReturn(Arrays.asList(campaign1, campaign2));
        
        List<CampaignDto> result = campaignService.getCampaignsByStatus(CampaignStatus.ACTIVE);
        
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(c -> c.getStatus() == CampaignStatus.ACTIVE));
    }
    
    @Test
    void testCreateCampaign_Success() {
        CampaignDto dto = CampaignDto.builder()
            .name("New Campaign")
            .description("Test Description")
            .status(CampaignStatus.DRAFT)
            .startDate(LocalDateTime.now())
            .endDate(LocalDateTime.now().plusDays(30))
            .build();
        
        CampaignEntity savedEntity = createCampaignEntity("generated-id", "New Campaign", CampaignStatus.DRAFT);
        
        when(campaignRepository.save(any(CampaignEntity.class))).thenReturn(savedEntity);
        
        CampaignDto result = campaignService.createCampaign(dto);
        
        assertNotNull(result);
        assertEquals("generated-id", result.getId());
        assertEquals("New Campaign", result.getName());
        verify(campaignRepository).save(any(CampaignEntity.class));
    }
    
    @Test
    void testCreateCampaign_NullStatus_DefaultsToDraft() {
        CampaignDto dto = CampaignDto.builder()
            .name("New Campaign")
            .description("Test")
            .status(null) // Null status
            .startDate(LocalDateTime.now())
            .endDate(LocalDateTime.now().plusDays(30))
            .build();
        
        CampaignEntity savedEntity = createCampaignEntity("id", "New Campaign", CampaignStatus.DRAFT);
        when(campaignRepository.save(any(CampaignEntity.class))).thenReturn(savedEntity);
        
        CampaignDto result = campaignService.createCampaign(dto);
        
        assertEquals(CampaignStatus.DRAFT, result.getStatus());
    }
    
    @Test
    void testUpdateCampaign_Success() {
        CampaignEntity existingEntity = createCampaignEntity("123", "Old Name", CampaignStatus.DRAFT);
        CampaignDto updateDto = CampaignDto.builder()
            .name("Updated Name")
            .description("Updated Description")
            .status(CampaignStatus.ACTIVE)
            .startDate(LocalDateTime.now())
            .endDate(LocalDateTime.now().plusDays(30))
            .build();
        
        CampaignEntity updatedEntity = createCampaignEntity("123", "Updated Name", CampaignStatus.ACTIVE);
        
        when(campaignRepository.findById("123")).thenReturn(Optional.of(existingEntity));
        when(campaignRepository.save(any(CampaignEntity.class))).thenReturn(updatedEntity);
        
        CampaignDto result = campaignService.updateCampaign("123", updateDto);
        
        assertEquals("Updated Name", result.getName());
        assertEquals(CampaignStatus.ACTIVE, result.getStatus());
        verify(campaignRepository).save(any(CampaignEntity.class));
    }
    
    @Test
    void testUpdateCampaign_NotFound_ThrowsException() {
        CampaignDto updateDto = CampaignDto.builder()
            .name("Updated Name")
            .build();
        
        when(campaignRepository.findById("nonexistent")).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> 
            campaignService.updateCampaign("nonexistent", updateDto)
        );
    }
    
    @Test
    void testDeleteCampaign_Success() {
        when(campaignRepository.existsById("123")).thenReturn(true);
        
        campaignService.deleteCampaign("123");
        
        verify(campaignRepository).deleteById("123");
    }
    
    @Test
    void testDeleteCampaign_NotFound_ThrowsException() {
        when(campaignRepository.existsById("nonexistent")).thenReturn(false);
        
        assertThrows(ResourceNotFoundException.class, () -> 
            campaignService.deleteCampaign("nonexistent")
        );
        
        verify(campaignRepository, never()).deleteById(anyString());
    }
    
    @Test
    void testActivateCampaign_Success() {
        CampaignEntity entity = createCampaignEntity("123", "Test", CampaignStatus.DRAFT);
        CampaignEntity activatedEntity = createCampaignEntity("123", "Test", CampaignStatus.ACTIVE);
        
        when(campaignRepository.findById("123")).thenReturn(Optional.of(entity));
        when(campaignRepository.save(any(CampaignEntity.class))).thenReturn(activatedEntity);
        
        CampaignDto result = campaignService.activateCampaign("123");
        
        assertEquals(CampaignStatus.ACTIVE, result.getStatus());
        verify(campaignRepository).save(any(CampaignEntity.class));
    }
    
    @Test
    void testEndCampaign_StopsAllActiveGames() {
        CampaignEntity entity = createCampaignEntity("123", "Test", CampaignStatus.ACTIVE);
        
        GameEntity game1 = createGameEntity("game-1", "123", GameStatus.ACTIVE);
        GameEntity game2 = createGameEntity("game-2", "123", GameStatus.ACTIVE);
        GameEntity game3 = createGameEntity("game-3", "123", GameStatus.ENDED);
        
        CampaignEntity endedEntity = createCampaignEntity("123", "Test", CampaignStatus.ENDED);
        
        when(campaignRepository.findById("123")).thenReturn(Optional.of(entity));
        when(gameRepository.findByCampaignId("123"))
            .thenReturn(Arrays.asList(game1, game2, game3));
        when(campaignRepository.save(any(CampaignEntity.class))).thenReturn(endedEntity);
        
        CampaignDto result = campaignService.endCampaign("123");
        
        assertEquals(CampaignStatus.ENDED, result.getStatus());
        verify(gameService, times(2)).stopGame(anyString()); // Only 2 active games
        verify(gameService).stopGame("game-1");
        verify(gameService).stopGame("game-2");
        verify(gameService, never()).stopGame("game-3"); // Already ended
    }
    
    @Test
    void testEndCampaign_HandlesGameStopErrors() {
        CampaignEntity entity = createCampaignEntity("123", "Test", CampaignStatus.ACTIVE);
        GameEntity game = createGameEntity("game-1", "123", GameStatus.ACTIVE);
        CampaignEntity endedEntity = createCampaignEntity("123", "Test", CampaignStatus.ENDED);
        
        when(campaignRepository.findById("123")).thenReturn(Optional.of(entity));
        when(gameRepository.findByCampaignId("123")).thenReturn(Collections.singletonList(game));
        when(campaignRepository.save(any(CampaignEntity.class))).thenReturn(endedEntity);
        doThrow(new RuntimeException("Stop error")).when(gameService).stopGame("game-1");
        
        CampaignDto result = campaignService.endCampaign("123");
        
        // Campaign should still be ended even if game stop fails
        assertEquals(CampaignStatus.ENDED, result.getStatus());
        verify(gameService).stopGame("game-1");
    }
    
    private CampaignEntity createCampaignEntity(String id, String name, CampaignStatus status) {
        CampaignEntity entity = new CampaignEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setDescription("Description for " + name);
        entity.setStatus(status);
        entity.setStartDate(LocalDateTime.now());
        entity.setEndDate(LocalDateTime.now().plusDays(30));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }
    
    private GameEntity createGameEntity(String id, String campaignId, GameStatus status) {
        GameEntity entity = new GameEntity();
        entity.setId(id);
        entity.setCampaignId(campaignId);
        entity.setStatus(status);
        return entity;
    }
}
