package com.frolic.services.controller.admin;

import com.frolic.core.common.dto.BrandDto;
import com.frolic.services.service.admin.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin controller for brand management
 */
@RestController
@RequestMapping("/api/v1/admin/brands")
@RequiredArgsConstructor
@Slf4j
public class BrandController {
    
    private final BrandService brandService;
    
    /**
     * Get all brands
     */
    @GetMapping
    public ResponseEntity<List<BrandDto>> getAllBrands(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        
        List<BrandDto> brands = activeOnly 
            ? brandService.getActiveBrands()
            : brandService.getAllBrands();
        
        return ResponseEntity.ok(brands);
    }
    
    /**
     * Get brand by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BrandDto> getBrandById(@PathVariable String id) {
        BrandDto brand = brandService.getBrandById(id);
        return ResponseEntity.ok(brand);
    }
    
    /**
     * Create new brand
     */
    @PostMapping
    public ResponseEntity<BrandDto> createBrand(@Valid @RequestBody BrandDto dto) {
        log.info("Creating brand: name={}", dto.getName());
        BrandDto created = brandService.createBrand(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    /**
     * Update brand
     */
    @PutMapping("/{id}")
    public ResponseEntity<BrandDto> updateBrand(
            @PathVariable String id,
            @Valid @RequestBody BrandDto dto) {
        
        log.info("Updating brand: id={}", id);
        BrandDto updated = brandService.updateBrand(id, dto);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * Delete brand
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBrand(@PathVariable String id) {
        log.info("Deleting brand: id={}", id);
        brandService.deleteBrand(id);
        return ResponseEntity.noContent().build();
    }
}
