-- V3: Backfill package weight and required vehicle category for packages created before V2,
-- using the same decision matrix as the application (defaults: bicycle <= 5 kg, K-Truck <= 350 kg, otherwise van;
-- unknown weight or full-truck bookings need at least a K-Truck).

UPDATE delivery d
SET package_weight_kg = sub.total_kg
FROM (
    SELECT d2.id, SUM(oi.quantity * p.weight_kg) AS total_kg, bool_and(p.weight_kg IS NOT NULL) AS all_known
    FROM delivery d2
    JOIN doch_market_delivery_request m ON m.id = d2.market_request_id
    JOIN order_item oi ON oi.order_id = m.order_id
    JOIN product p ON p.id = oi.product_id AND p.seller_id = m.seller_id
    GROUP BY d2.id
) sub
WHERE d.id = sub.id AND d.package_weight_kg IS NULL AND sub.all_known;

UPDATE delivery d
SET required_vehicle_category = CASE
        WHEN d.package_weight_kg IS NULL THEN 'K_TRUCK'
        WHEN d.package_weight_kg <= 5 AND COALESCE(o.transport_type, x.transport_type, 'SINGLE_PARCEL') <> 'FULL_TRUCK' THEN 'BICYCLE'
        WHEN d.package_weight_kg <= 350 THEN 'K_TRUCK'
        ELSE 'VAN'
    END
FROM delivery d0
LEFT JOIN doch_market_delivery_request m ON m.id = d0.market_request_id
LEFT JOIN t_orders o ON o.id = m.order_id
LEFT JOIN doch_xender_delivery_request x ON x.id = d0.xender_request_id
WHERE d.id = d0.id AND d.required_vehicle_category IS NULL;
