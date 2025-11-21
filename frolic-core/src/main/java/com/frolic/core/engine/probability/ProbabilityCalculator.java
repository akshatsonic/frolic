package com.frolic.core.engine.probability;

import com.frolic.core.common.util.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * Probability calculator for reward allocation
 * Implements time-based probability distribution algorithm
 */
@Component
@Slf4j
public class ProbabilityCalculator {
    
    private final Random random = new Random();
    
    /**
     * Calculate probability for a play event
     * 
     * P_base = remainingBudget / remainingSlots
     * 
     * If P_base < 1:
     *   Probabilistic: if random() < P_base then allocate 1 coupon
     * Else:
     *   Deterministic + fractional: allocate floor(P_base) + (random() < fractional ? 1 : 0)
     * 
     * @param remainingBudget Number of coupons left to allocate
     * @param startTime Game start time
     * @param endTime Game end time
     * @param slotGranularitySeconds Slot duration in seconds
     * @return Number of coupons to allocate for this play
     */
    public int calculateAllocation(int remainingBudget, LocalDateTime startTime, LocalDateTime endTime, int slotGranularitySeconds) {
        if (remainingBudget <= 0) {
            return 0;
        }
        
        long remainingSlots = calculateRemainingSlots(startTime, endTime, slotGranularitySeconds);
        
        if (remainingSlots <= 0) {
            log.warn("No remaining slots, allocating all remaining budget: {}", remainingBudget);
            return remainingBudget;
        }
        
        double pBase = (double) remainingBudget / remainingSlots;
        
        if (pBase < 1.0) {
            // Probabilistic allocation
            return random.nextDouble() < pBase ? 1 : 0;
        } else {
            // Deterministic + fractional allocation
            int fixedWinners = (int) Math.floor(pBase);
            double fractional = pBase - fixedWinners;
            int additionalWinner = random.nextDouble() < fractional ? 1 : 0;
            int totalAllocation = fixedWinners + additionalWinner;
            
            // Cap at remaining budget
            return Math.min(totalAllocation, remainingBudget);
        }
    }
    
    /**
     * Calculate remaining time slots until game end
     */
    public long calculateRemainingSlots(LocalDateTime startTime, LocalDateTime endTime, int slotGranularitySeconds) {
        long remainingSeconds = TimeUtils.remainingSeconds(endTime);
        
        if (remainingSeconds <= 0) {
            return 0;
        }
        
        return Math.max(1, remainingSeconds / slotGranularitySeconds);
    }
    
    /**
     * Check if allocation should be made based on probability
     */
    public boolean shouldAllocate(double probability) {
        return random.nextDouble() < probability;
    }
}
