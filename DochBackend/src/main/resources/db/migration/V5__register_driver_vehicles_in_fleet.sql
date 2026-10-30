-- Drivers declare a vehicle (type + plate) when they are onboarded. Until now that vehicle was only stored on the
-- driver profile, so dispatch had no fleet vehicle to assign. Register those vehicles in the fleet, assigned to
-- their driver (same category mapping as VehicleCategory.fromVehicleType). Admins can adjust them under Reports > Fleet.
INSERT INTO fleet_vehicle (id, created_at, updated_at, is_deleted, name, vehicle_type, category, plate_number, status,
                           assigned_driver_profile_id, notes)
SELECT gen_random_uuid()::text,
       now(),
       now(),
       false,
       CASE c.category WHEN 'BICYCLE' THEN 'Bicycle ' WHEN 'VAN' THEN 'Toyota HiAce ' ELSE 'Suzuki Carry / K-Truck ' END
           || trim(p.plate_number),
       p.vehicle_type,
       c.category,
       trim(p.plate_number),
       'ACTIVE',
       p.id,
       'Registered automatically from the driver''s onboarding details.'
FROM driver_profile p
         JOIN users u ON u.id = p.user_id AND u.is_deleted = false
         CROSS JOIN LATERAL (
    SELECT CASE
               WHEN upper(trim(coalesce(p.vehicle_type, ''))) IN ('BICYCLE', 'K_TRUCK', 'VAN')
                   THEN upper(trim(p.vehicle_type))
               WHEN lower(coalesce(p.vehicle_type, '')) ~ '(bicycle|bike|velo|vélo|cycle|moto|scooter)' THEN 'BICYCLE'
               WHEN lower(coalesce(p.vehicle_type, '')) ~ '(van|hiace|bus|lorry|heavy|full)' THEN 'VAN'
               ELSE 'K_TRUCK'
               END AS category
    ) c
WHERE coalesce(trim(p.plate_number), '') <> ''
  AND NOT EXISTS (SELECT 1 FROM fleet_vehicle v WHERE v.plate_number = trim(p.plate_number))
  AND NOT EXISTS (SELECT 1 FROM fleet_vehicle v WHERE v.assigned_driver_profile_id = p.id);
