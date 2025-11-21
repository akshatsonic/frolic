package com.frolic.services.controller.play.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Play request payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayRequest {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotBlank(message = "Game ID is required")
    private String gameId;
    
    private Map<String, Object> metadata;
}
