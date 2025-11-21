package com.frolic.services.service.admin;

import com.frolic.core.common.dto.BrandDto;
import com.frolic.core.common.exception.ResourceNotFoundException;
import com.frolic.core.repository.entity.BrandEntity;
import com.frolic.core.repository.jpa.BrandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for brand management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BrandService {
    
    private final BrandRepository brandRepository;
    
    /**
     * Get all brands
     */
    public List<BrandDto> getAllBrands() {
        return brandRepository.findAll().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Get active brands
     */
    public List<BrandDto> getActiveBrands() {
        return brandRepository.findByActiveOrderByNameAsc(true).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Get brand by ID
     */
    public BrandDto getBrandById(String id) {
        BrandEntity entity = brandRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Brand", id));
        return toDto(entity);
    }
    
    /**
     * Create a new brand
     */
    @Transactional
    public BrandDto createBrand(BrandDto dto) {
        BrandEntity entity = new BrandEntity();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setLogoUrl(dto.getLogoUrl());
        entity.setActive(true);
        
        entity = brandRepository.save(entity);
        log.info("Created brand: id={}, name={}", entity.getId(), entity.getName());
        
        return toDto(entity);
    }
    
    /**
     * Update existing brand
     */
    @Transactional
    public BrandDto updateBrand(String id, BrandDto dto) {
        BrandEntity entity = brandRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Brand", id));
        
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setLogoUrl(dto.getLogoUrl());
        entity.setActive(dto.isActive());
        
        entity = brandRepository.save(entity);
        log.info("Updated brand: id={}, name={}", entity.getId(), entity.getName());
        
        return toDto(entity);
    }
    
    /**
     * Delete brand
     */
    @Transactional
    public void deleteBrand(String id) {
        if (!brandRepository.existsById(id)) {
            throw new ResourceNotFoundException("Brand", id);
        }
        brandRepository.deleteById(id);
        log.info("Deleted brand: id={}", id);
    }
    
    private BrandDto toDto(BrandEntity entity) {
        return BrandDto.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(entity.getDescription())
            .logoUrl(entity.getLogoUrl())
            .active(entity.isActive())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
