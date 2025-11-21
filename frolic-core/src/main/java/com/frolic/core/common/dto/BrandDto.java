package com.frolic.core.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for brand data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandDto {
    
    private String id;
    private String name;
    private String description;
    private String logoUrl;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
