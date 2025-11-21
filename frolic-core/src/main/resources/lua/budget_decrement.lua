-- budget_decrement.lua
-- Atomic budget decrement script for Redis
-- KEYS[1] = budget key (budget:game:{gameId}:brand:{brandId})
-- ARGV[1] = decrement amount (number of coupons to allocate)
-- Returns: remaining budget after decrement, or -1 if insufficient budget

local current = tonumber(redis.call('GET', KEYS[1]) or '0')
local amount = tonumber(ARGV[1])

if current >= amount then
    redis.call('DECRBY', KEYS[1], amount)
    return current - amount
else
    return -1
end
