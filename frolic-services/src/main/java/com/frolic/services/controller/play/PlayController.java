package com.frolic.services.controller.play;

import com.frolic.services.controller.play.request.PlayRequest;
import com.frolic.services.controller.play.response.PlayResponse;
import com.frolic.services.service.play.PlayIngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for play ingestion API
 */
@RestController
@RequestMapping("/api/v1/play")
@RequiredArgsConstructor
@Slf4j
public class PlayController {
    
    private final PlayIngestionService playIngestionService;
    
    /**
     * Submit a play request
     * Returns 202 Accepted with playId
     */
    @PostMapping
    public ResponseEntity<PlayResponse> submitPlay(@Valid @RequestBody PlayRequest request) {
        log.info("Received play request: userId={}, gameId={}", request.getUserId(), request.getGameId());
        
        PlayResponse response = playIngestionService.submitPlay(request);
        
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    
    /**
     * Get play result by playId
     */
    @GetMapping("/{playId}/result")
    public ResponseEntity<PlayResponse> getPlayResult(@PathVariable String playId) {
        log.debug("Fetching result for playId={}", playId);
        
        PlayResponse response = playIngestionService.getPlayResult(playId);
        
        return ResponseEntity.ok(response);
    }
}
