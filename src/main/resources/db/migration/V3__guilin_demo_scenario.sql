-- Keep V1 unchanged so existing databases pass Flyway checksum validation.
-- Fictional demonstration stations; coordinates are illustrative, not real aid locations.
UPDATE shelter
SET name = '象山社区共享站', district = '象山区',
    latitude = 25.2600000, longitude = 110.2800000, version = version + 1
WHERE id = 1 AND name = '灯塔共享站' AND district = '浦东新区';

UPDATE shelter
SET name = '七星社区物资仓', district = '七星区',
    latitude = 25.2600000, longitude = 110.3100000, version = version + 1
WHERE id = 2 AND name = '星河社区仓' AND district = '徐汇区';

-- Preserve request IDs, allocation links and inventory while moving the demo scenario.
UPDATE relief_request
SET district = CASE district WHEN '浦东新区' THEN '象山区' ELSE '七星区' END,
    version = version + 1
WHERE district IN ('浦东新区', '徐汇区');
