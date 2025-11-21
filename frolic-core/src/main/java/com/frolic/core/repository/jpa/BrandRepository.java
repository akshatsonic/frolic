package com.frolic.core.repository.jpa;

import com.frolic.core.repository.entity.BrandEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Brand entity
 */
@Repository
public interface BrandRepository extends JpaRepository<BrandEntity, String> {
    
    Optional<BrandEntity> findByName(String name);
    
    List<BrandEntity> findByActive(boolean active);
    
    List<BrandEntity> findByActiveOrderByNameAsc(boolean active);
}
