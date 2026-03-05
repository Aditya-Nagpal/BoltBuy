-- KEYS[1]: product_stock_key (e.g., "product:1:stock")
-- ARGV[1]: quantity_to_deduct (usually 1)

local current_stock = redis.call("GET", KEYS[1])

if current_stock and tonumber(current_stock) >= tonumber(ARGV[1]) then
    redis.call("DECRBY", KEYS[1], ARGV[1])
    return 1 -- Success
else
    return 0 -- Failure: Out of stock or product not found
end