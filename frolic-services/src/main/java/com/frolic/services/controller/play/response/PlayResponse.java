package com.frolic.services.controller.play.response;

import com.frolic.core.common.enums.PlayStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Play response payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayResponse {
    
    private String playId;
    private String gameId;
    private String userId;
    private PlayStatus status;
    private Boolean winner;
    private String couponCode;
    private String brandName;
    private String message;
}
