package com.frolic.services.controller.admin;

import com.frolic.core.common.dto.GameDto;
import com.frolic.core.common.enums.GameStatus;
import com.frolic.services.service.admin.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin controller for game management
 */
@RestController
@RequestMapping("/api/v1/admin/games")
@RequiredArgsConstructor
@Slf4j
public class GameController {
    
    private final GameService gameService;
    
    @GetMapping
    public ResponseEntity<List<GameDto>> getAllGames(
            @RequestParam(required = false) String campaignId,
            @RequestParam(required = false) GameStatus status) {
        
        List<GameDto> games;
        if (campaignId != null) {
            games = gameService.getGamesByCampaign(campaignId);
        } else if (status != null) {
            games = gameService.getGamesByStatus(status);
        } else {
            games = gameService.getAllGames();
        }
        
        return ResponseEntity.ok(games);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<GameDto> getGameById(@PathVariable String id) {
        GameDto game = gameService.getGameById(id);
        return ResponseEntity.ok(game);
    }
    
    @PostMapping
    public ResponseEntity<GameDto> createGame(@Valid @RequestBody GameDto dto) {
        log.info("Creating game: name={}", dto.getName());
        GameDto created = gameService.createGame(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<GameDto> updateGame(@PathVariable String id, @Valid @RequestBody GameDto dto) {
        log.info("Updating game: id={}", id);
        GameDto updated = gameService.updateGame(id, dto);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable String id) {
        log.info("Deleting game: id={}", id);
        gameService.deleteGame(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/start")
    public ResponseEntity<GameDto> startGame(@PathVariable String id) {
        log.info("Starting game: id={}", id);
        GameDto started = gameService.startGame(id);
        return ResponseEntity.ok(started);
    }
    
    @PostMapping("/{id}/stop")
    public ResponseEntity<GameDto> stopGame(@PathVariable String id) {
        log.info("Stopping game: id={}", id);
        GameDto stopped = gameService.stopGame(id);
        return ResponseEntity.ok(stopped);
    }
    
    @PostMapping("/{id}/pause")
    public ResponseEntity<GameDto> pauseGame(@PathVariable String id) {
        log.info("Pausing game: id={}", id);
        GameDto paused = gameService.pauseGame(id);
        return ResponseEntity.ok(paused);
    }
    
    @PostMapping("/{id}/resume")
    public ResponseEntity<GameDto> resumeGame(@PathVariable String id) {
        log.info("Resuming game: id={}", id);
        GameDto resumed = gameService.resumeGame(id);
        return ResponseEntity.ok(resumed);
    }
}
