package com.frolic.core.engine.probability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProbabilityCalculator
 */
class ProbabilityCalculatorTest {
    
    private ProbabilityCalculator calculator;
    
    @BeforeEach
    void setUp() {
        calculator = new ProbabilityCalculator();
    }
    
    @Test
    void testCalculateAllocation_ZeroBudget_ReturnsZero() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(1);
        
        int allocation = calculator.calculateAllocation(0, start, end, 60);
        
        assertEquals(0, allocation, "Zero budget should return zero allocation");
    }
    
    @Test
    void testCalculateAllocation_NegativeBudget_ReturnsZero() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(1);
        
        int allocation = calculator.calculateAllocation(-10, start, end, 60);
        
        assertEquals(0, allocation, "Negative budget should return zero allocation");
    }
    
    @Test
    void testCalculateAllocation_NoRemainingSlots_ReturnsAllBudget() {
        LocalDateTime start = LocalDateTime.now().minusHours(2);
        LocalDateTime end = LocalDateTime.now().minusHours(1);
        
        int allocation = calculator.calculateAllocation(100, start, end, 60);
        
        assertEquals(100, allocation, "No remaining slots should allocate all remaining budget");
    }
    
    @Test
    void testCalculateAllocation_ProbabilisticMode_ReturnsZeroOrOne() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(10);
        int remainingBudget = 100;
        int slotGranularity = 60;
        
        // With 10 hours and 60-second slots, we have 600 slots
        // P_base = 100/600 = 0.166 < 1.0 (probabilistic mode)
        
        Map<Integer, Integer> results = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            int allocation = calculator.calculateAllocation(remainingBudget, start, end, slotGranularity);
            results.put(allocation, results.getOrDefault(allocation, 0) + 1);
        }
        
        // Should only return 0 or 1
        assertTrue(results.keySet().stream().allMatch(k -> k == 0 || k == 1),
            "Probabilistic mode should only return 0 or 1");
        
        // Both values should occur (with high probability)
        assertTrue(results.containsKey(0), "Should have some 0 allocations");
        assertTrue(results.containsKey(1), "Should have some 1 allocations");
        
        // Approximately 16-17% should be 1 (based on P_base ≈ 0.166)
        double winRate = results.get(1) / 1000.0;
        assertTrue(winRate > 0.10 && winRate < 0.25,
            "Win rate should be approximately 16.6%, got: " + winRate);
    }
    
    @Test
    void testCalculateAllocation_DeterministicMode_ReturnsMultiple() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusMinutes(30);
        int remainingBudget = 100;
        int slotGranularity = 60;
        
        // With 30 minutes and 60-second slots, we have 30 slots
        // P_base = 100/30 = 3.33 > 1.0 (deterministic mode)
        
        int allocation = calculator.calculateAllocation(remainingBudget, start, end, slotGranularity);
        
        assertTrue(allocation >= 3 && allocation <= 4,
            "Should allocate floor(3.33) = 3 or 4 coupons, got: " + allocation);
    }
    
    @Test
    void testCalculateAllocation_CappedAtRemainingBudget() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusSeconds(10);
        int remainingBudget = 5;
        int slotGranularity = 1;
        
        // Very few slots remaining, high P_base
        int allocation = calculator.calculateAllocation(remainingBudget, start, end, slotGranularity);
        
        assertTrue(allocation <= remainingBudget,
            "Allocation should never exceed remaining budget");
    }
    
    @ParameterizedTest
    @CsvSource({
        "3600, 60, 60",      // 1 hour, 60s slots = 60 slots
        "7200, 120, 60",     // 2 hours, 120s slots = 60 slots
        "1800, 30, 60",      // 30 minutes, 30s slots = 60 slots
        "600, 60, 10"        // 10 minutes, 60s slots = 10 slots
    })
    void testCalculateRemainingSlots(long durationSeconds, int slotGranularity, long expectedSlots) {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusSeconds(durationSeconds);
        
        long slots = calculator.calculateRemainingSlots(start, end, slotGranularity);
        
        assertTrue(Math.abs(slots - expectedSlots) <= 1,
            String.format("Expected %d slots, got %d", expectedSlots, slots));
    }
    
    @Test
    void testCalculateRemainingSlots_PastEndTime_ReturnsZero() {
        LocalDateTime start = LocalDateTime.now().minusHours(2);
        LocalDateTime end = LocalDateTime.now().minusHours(1);
        
        long slots = calculator.calculateRemainingSlots(start, end, 60);
        
        assertEquals(0, slots, "Past end time should return 0 slots");
    }
    
    @Test
    void testCalculateRemainingSlots_MinimumOneSlot() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusSeconds(30);
        
        long slots = calculator.calculateRemainingSlots(start, end, 60);
        
        assertTrue(slots >= 1, "Should return at least 1 slot if any time remaining");
    }
    
    @Test
    void testShouldAllocate_ZeroProbability_ReturnsFalse() {
        boolean result = calculator.shouldAllocate(0.0);
        assertFalse(result, "Zero probability should always return false");
    }
    
    @Test
    void testShouldAllocate_OneProbability_ReturnsTrue() {
        boolean result = calculator.shouldAllocate(1.0);
        assertTrue(result, "Probability of 1.0 should always return true");
    }
    
    @Test
    void testShouldAllocate_MidProbability_DistributionCheck() {
        double probability = 0.5;
        int trueCount = 0;
        int iterations = 1000;
        
        for (int i = 0; i < iterations; i++) {
            if (calculator.shouldAllocate(probability)) {
                trueCount++;
            }
        }
        
        double actualProbability = trueCount / (double) iterations;
        assertTrue(actualProbability > 0.4 && actualProbability < 0.6,
            "50% probability should result in ~50% true rate, got: " + actualProbability);
    }
    
    @Test
    void testShouldAllocate_LowProbability_DistributionCheck() {
        double probability = 0.1;
        int trueCount = 0;
        int iterations = 10000;
        
        for (int i = 0; i < iterations; i++) {
            if (calculator.shouldAllocate(probability)) {
                trueCount++;
            }
        }
        
        double actualProbability = trueCount / (double) iterations;
        assertTrue(actualProbability > 0.08 && actualProbability < 0.12,
            "10% probability should result in ~10% true rate, got: " + actualProbability);
    }
}
