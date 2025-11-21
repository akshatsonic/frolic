package com.frolic.core.common.dto;

import com.frolic.core.common.enums.PlayStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for play event data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayEventDto {
    
    private String playId;
    private String gameId;
    private String userId;
    private PlayStatus status;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
}
