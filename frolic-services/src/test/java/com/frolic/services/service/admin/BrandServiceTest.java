package com.frolic.services.service.admin;

import com.frolic.core.common.dto.BrandDto;
import com.frolic.core.common.exception.ResourceNotFoundException;
import com.frolic.core.repository.entity.BrandEntity;
import com.frolic.core.repository.jpa.BrandRepository;
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
 * Unit tests for BrandService
 */
@ExtendWith(MockitoExtension.class)
class BrandServiceTest {
    
    @Mock
    private BrandRepository brandRepository;
    
    @InjectMocks
    private BrandService brandService;
    
    @Test
    void testGetAllBrands_ReturnsAllBrands() {
        BrandEntity brand1 = createBrandEntity("brand-1", "Brand 1", true);
        BrandEntity brand2 = createBrandEntity("brand-2", "Brand 2", false);
        
        when(brandRepository.findAll()).thenReturn(Arrays.asList(brand1, brand2));
        
        List<BrandDto> result = brandService.getAllBrands();
        
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("brand-1");
        assertThat(result.get(1).getId()).isEqualTo("brand-2");
        
        verify(brandRepository).findAll();
    }
    
    @Test
    void testGetActiveBrands_ReturnsOnlyActiveBrands() {
        BrandEntity brand1 = createBrandEntity("brand-1", "Brand 1", true);
        
        when(brandRepository.findByActiveOrderByNameAsc(true)).thenReturn(Arrays.asList(brand1));
        
        List<BrandDto> result = brandService.getActiveBrands();
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("brand-1");
        assertThat(result.get(0).isActive()).isTrue();
        
        verify(brandRepository).findByActiveOrderByNameAsc(true);
    }
    
    @Test
    void testGetBrandById_ValidId_ReturnsBrand() {
        BrandEntity brand = createBrandEntity("brand-1", "Brand 1", true);
        
        when(brandRepository.findById("brand-1")).thenReturn(Optional.of(brand));
        
        BrandDto result = brandService.getBrandById("brand-1");
        
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("brand-1");
        assertThat(result.getName()).isEqualTo("Brand 1");
        
        verify(brandRepository).findById("brand-1");
    }
    
    @Test
    void testGetBrandById_InvalidId_ThrowsResourceNotFoundException() {
        when(brandRepository.findById("invalid-id")).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> brandService.getBrandById("invalid-id"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Brand")
            .hasMessageContaining("invalid-id");
        
        verify(brandRepository).findById("invalid-id");
    }
    
    @Test
    void testCreateBrand_ValidDto_CreatesBrand() {
        BrandDto dto = BrandDto.builder()
            .name("New Brand")
            .description("Test description")
            .logoUrl("http://logo.png")
            .build();
        
        BrandEntity savedEntity = createBrandEntity("brand-1", "New Brand", true);
        
        when(brandRepository.save(any(BrandEntity.class))).thenReturn(savedEntity);
        
        BrandDto result = brandService.createBrand(dto);
        
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("brand-1");
        assertThat(result.getName()).isEqualTo("New Brand");
        assertThat(result.isActive()).isTrue();
        
        verify(brandRepository).save(any(BrandEntity.class));
    }
    
    @Test
    void testUpdateBrand_ValidIdAndDto_UpdatesBrand() {
        BrandEntity existingEntity = createBrandEntity("brand-1", "Old Brand", true);
        
        when(brandRepository.findById("brand-1")).thenReturn(Optional.of(existingEntity));
        when(brandRepository.save(any(BrandEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        BrandDto dto = BrandDto.builder()
            .name("Updated Brand")
            .description("Updated description")
            .logoUrl("http://newlogo.png")
            .active(false)
            .build();
        
        BrandDto result = brandService.updateBrand("brand-1", dto);
        
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Brand");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        assertThat(result.isActive()).isFalse();
        
        verify(brandRepository).findById("brand-1");
        verify(brandRepository).save(any(BrandEntity.class));
    }
    
    @Test
    void testUpdateBrand_InvalidId_ThrowsResourceNotFoundException() {
        when(brandRepository.findById("invalid-id")).thenReturn(Optional.empty());
        
        BrandDto dto = BrandDto.builder()
            .name("Updated Brand")
            .build();
        
        assertThatThrownBy(() -> brandService.updateBrand("invalid-id", dto))
            .isInstanceOf(ResourceNotFoundException.class);
        
        verify(brandRepository).findById("invalid-id");
        verify(brandRepository, never()).save(any(BrandEntity.class));
    }
    
    @Test
    void testDeleteBrand_ValidId_DeletesBrand() {
        when(brandRepository.existsById("brand-1")).thenReturn(true);
        doNothing().when(brandRepository).deleteById("brand-1");
        
        assertThatCode(() -> brandService.deleteBrand("brand-1"))
            .doesNotThrowAnyException();
        
        verify(brandRepository).existsById("brand-1");
        verify(brandRepository).deleteById("brand-1");
    }
    
    @Test
    void testDeleteBrand_InvalidId_ThrowsResourceNotFoundException() {
        when(brandRepository.existsById("invalid-id")).thenReturn(false);
        
        assertThatThrownBy(() -> brandService.deleteBrand("invalid-id"))
            .isInstanceOf(ResourceNotFoundException.class);
        
        verify(brandRepository).existsById("invalid-id");
        verify(brandRepository, never()).deleteById(any());
    }
    
    private BrandEntity createBrandEntity(String id, String name, boolean active) {
        BrandEntity entity = new BrandEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setDescription("Test description");
        entity.setLogoUrl("http://logo.png");
        entity.setActive(active);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }
}
