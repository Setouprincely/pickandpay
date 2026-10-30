-- V2: Workflow hardening for staging acceptance.
--  * product publication workflow + normalised weight/dimensions
--  * order references, payment lifecycle columns, optimistic locking
--  * package tracking numbers, vehicle decision matrix, dispatch/hub/exception columns
--  * tracking history (delivery_event), audit trail (audit_log), reviews (order_review)
-- All statements are idempotent so they can run on databases that were previously
-- managed by Hibernate ddl-auto (baselined at V1).

-- ---------------------------------------------------------------- product
ALTER TABLE product ALTER COLUMN description TYPE varchar(2000);
ALTER TABLE product ADD COLUMN IF NOT EXISTS weight_kg double precision;
ALTER TABLE product ADD COLUMN IF NOT EXISTS length_cm double precision;
ALTER TABLE product ADD COLUMN IF NOT EXISTS width_cm double precision;
ALTER TABLE product ADD COLUMN IF NOT EXISTS height_cm double precision;
ALTER TABLE product ADD COLUMN IF NOT EXISTS publication_status varchar(30);
ALTER TABLE product ADD COLUMN IF NOT EXISTS rejection_reason varchar(1000);
ALTER TABLE product ADD COLUMN IF NOT EXISTS reviewed_at timestamp(6) without time zone;
ALTER TABLE product ADD COLUMN IF NOT EXISTS reviewed_by varchar(36);
ALTER TABLE product ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;

-- Listings that existed before the moderation workflow keep their current visibility.
UPDATE product SET publication_status = 'PUBLISHED' WHERE publication_status IS NULL;

-- Normalise free-text weights such as "2", "2.5kg", "500 g" into kilograms.
UPDATE product
SET weight_kg = CASE
        WHEN lower(weight) ~ '[0-9.]+\s*g\s*$' AND lower(weight) !~ 'kg'
            THEN (substring(weight FROM '([0-9]+(\.[0-9]+)?)'))::double precision / 1000.0
        ELSE (substring(weight FROM '([0-9]+(\.[0-9]+)?)'))::double precision
    END
WHERE weight_kg IS NULL AND weight ~ '[0-9]';

ALTER TABLE product DROP CONSTRAINT IF EXISTS product_publication_status_check;
ALTER TABLE product ADD CONSTRAINT product_publication_status_check
    CHECK (publication_status IN ('PENDING_REVIEW', 'PUBLISHED', 'REJECTED'));
ALTER TABLE product DROP CONSTRAINT IF EXISTS product_price_check;
ALTER TABLE product ADD CONSTRAINT product_price_check CHECK (price >= 0);
ALTER TABLE product DROP CONSTRAINT IF EXISTS product_stock_check;
ALTER TABLE product ADD CONSTRAINT product_stock_check CHECK (stock_quantity >= 0);
CREATE INDEX IF NOT EXISTS idx_product_publication ON product (publication_status, is_activity);
CREATE INDEX IF NOT EXISTS idx_product_seller ON product (seller_id);

-- ---------------------------------------------------------------- seller
ALTER TABLE seller ADD COLUMN IF NOT EXISTS verified_at timestamp(6) without time zone;
ALTER TABLE seller ADD COLUMN IF NOT EXISTS verification_note varchar(500);

-- ---------------------------------------------------------------- orders
ALTER TABLE t_orders ADD COLUMN IF NOT EXISTS reference varchar(20);
ALTER TABLE t_orders ADD COLUMN IF NOT EXISTS contact_phone varchar(30);
ALTER TABLE t_orders ADD COLUMN IF NOT EXISTS paid_at timestamp(6) without time zone;
ALTER TABLE t_orders ADD COLUMN IF NOT EXISTS cancelled_at timestamp(6) without time zone;
ALTER TABLE t_orders ADD COLUMN IF NOT EXISTS cancellation_reason varchar(500);
ALTER TABLE t_orders ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;

UPDATE t_orders SET reference = 'DO-' || upper(substr(md5(id), 1, 8)) WHERE reference IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_reference ON t_orders (reference);
CREATE INDEX IF NOT EXISTS idx_orders_customer ON t_orders (customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_status_created ON t_orders (status, created_at);

ALTER TABLE t_orders DROP CONSTRAINT IF EXISTS t_orders_status_check;
ALTER TABLE t_orders ADD CONSTRAINT t_orders_status_check CHECK (status IN (
    'PENDING', 'PAYMENT_FAILED', 'PROCESSING', 'COMPLETED', 'CANCELLED', 'SHIPPED',
    'IN_TRANSIT', 'DELIVERED', 'RETURNED', 'REFUNDED'));

CREATE INDEX IF NOT EXISTS idx_order_item_order ON order_item (order_id);
CREATE INDEX IF NOT EXISTS idx_market_request_order ON doch_market_delivery_request (order_id);

-- ---------------------------------------------------------------- payment
ALTER TABLE payment ADD COLUMN IF NOT EXISTS payment_flow varchar(10);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS payer_phone varchar(20);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS provider_status varchar(20);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS provider_checked_at timestamp(6) without time zone;
ALTER TABLE payment ADD COLUMN IF NOT EXISTS paid_at timestamp(6) without time zone;
ALTER TABLE payment ADD COLUMN IF NOT EXISTS failure_reason varchar(500);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;
UPDATE payment SET payment_flow = 'LINK' WHERE payment_flow IS NULL;
CREATE INDEX IF NOT EXISTS idx_payment_order ON payment (order_id);
CREATE INDEX IF NOT EXISTS idx_payment_xender ON payment (xender_request_id);
CREATE INDEX IF NOT EXISTS idx_payment_status ON payment (status);
CREATE INDEX IF NOT EXISTS idx_payment_gateway_tx ON payment (gateway_transaction_id);

UPDATE t_orders o SET paid_at = p.updated_at
FROM payment p
WHERE p.order_id = o.id AND p.status = 'COMPLETED' AND o.paid_at IS NULL;

-- ---------------------------------------------------------------- fleet
ALTER TABLE fleet_vehicle ADD COLUMN IF NOT EXISTS category varchar(20);
ALTER TABLE fleet_vehicle ADD COLUMN IF NOT EXISTS max_load_kg double precision;
ALTER TABLE fleet_vehicle DROP CONSTRAINT IF EXISTS fleet_vehicle_category_check;
ALTER TABLE fleet_vehicle ADD CONSTRAINT fleet_vehicle_category_check
    CHECK (category IS NULL OR category IN ('BICYCLE', 'K_TRUCK', 'VAN'));

-- ---------------------------------------------------------------- settings
ALTER TABLE pricing_settings ADD COLUMN IF NOT EXISTS bicycle_max_weight_kg double precision DEFAULT 5;
ALTER TABLE pricing_settings ADD COLUMN IF NOT EXISTS bicycle_max_dimension_cm double precision DEFAULT 60;
ALTER TABLE pricing_settings ADD COLUMN IF NOT EXISTS k_truck_max_weight_kg double precision DEFAULT 350;
ALTER TABLE pricing_settings ADD COLUMN IF NOT EXISTS product_moderation_required boolean DEFAULT true;
UPDATE pricing_settings SET bicycle_max_weight_kg = 5 WHERE bicycle_max_weight_kg IS NULL;
UPDATE pricing_settings SET bicycle_max_dimension_cm = 60 WHERE bicycle_max_dimension_cm IS NULL;
UPDATE pricing_settings SET k_truck_max_weight_kg = 350 WHERE k_truck_max_weight_kg IS NULL;
UPDATE pricing_settings SET product_moderation_required = true WHERE product_moderation_required IS NULL;

-- ---------------------------------------------------------------- delivery
ALTER TABLE delivery ADD COLUMN IF NOT EXISTS tracking_number varchar(20);
ALTER TABLE delivery ADD COLUMN IF NOT EXISTS otp_failed_attempts integer NOT NULL DEFAULT 0;
ALTER TABLE delivery ADD COLUMN IF NOT EXISTS package_weight_kg double precision;
ALTER TABLE delivery ADD COLUMN IF NOT EXISTS package_max_dimension_cm double precision;
ALTER TABLE delivery ADD COLUMN IF NOT EXISTS required_vehicle_category varchar(20);
ALTER TABLE delivery ADD COLUMN IF NOT EXISTS vehicle_id varchar(36);
ALTER TABLE delivery ADD COLUMN IF NOT EXISTS assigned_at timestamp(6) without time zone;
ALTER TABLE delivery ADD COLUMN IF NOT EXISTS scheduled_for timestamp(6) without time zone;
ALTER TABLE delivery ADD COLUMN IF NOT EXISTS failure_reason varchar(500);
ALTER TABLE delivery ADD COLUMN IF NOT EXISTS current_hub varchar(120);
ALTER TABLE delivery ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;

UPDATE delivery SET tracking_number = 'DCH-' || upper(substr(md5(id), 1, 10)) WHERE tracking_number IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_delivery_tracking ON delivery (tracking_number);
CREATE INDEX IF NOT EXISTS idx_delivery_status ON delivery (status);
CREATE INDEX IF NOT EXISTS idx_delivery_driver ON delivery (driver_id);
CREATE INDEX IF NOT EXISTS idx_delivery_market_request ON delivery (market_request_id);
CREATE INDEX IF NOT EXISTS idx_delivery_xender_request ON delivery (xender_request_id);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_delivery_vehicle') THEN
        ALTER TABLE delivery ADD CONSTRAINT fk_delivery_vehicle FOREIGN KEY (vehicle_id) REFERENCES fleet_vehicle (id);
    END IF;
END $$;

ALTER TABLE delivery DROP CONSTRAINT IF EXISTS delivery_status_check;
ALTER TABLE delivery ADD CONSTRAINT delivery_status_check CHECK (status IN (
    'PENDING_PAYMENT', 'PENDING_PREPARATION', 'PENDING_ASSIGNMENT', 'DRIVER_ASSIGNED', 'PICKED_UP',
    'AT_HUB', 'IN_TRANSIT', 'DELIVERED', 'FAILED', 'RETURNED', 'CANCELLED'));
ALTER TABLE delivery DROP CONSTRAINT IF EXISTS delivery_required_vehicle_category_check;
ALTER TABLE delivery ADD CONSTRAINT delivery_required_vehicle_category_check
    CHECK (required_vehicle_category IS NULL OR required_vehicle_category IN ('BICYCLE', 'K_TRUCK', 'VAN'));

-- ---------------------------------------------------------------- tracking history
CREATE TABLE IF NOT EXISTS delivery_event (
    id              varchar(36) NOT NULL PRIMARY KEY,
    delivery_id     varchar(36) NOT NULL REFERENCES delivery (id),
    event_type      varchar(40) NOT NULL,
    from_status     varchar(30),
    to_status       varchar(30),
    public_message  varchar(500),
    internal_note   varchar(1000),
    actor_id        varchar(36),
    actor_name      varchar(200),
    actor_role      varchar(50),
    hub_name        varchar(120),
    latitude        varchar(255),
    longitude       varchar(255),
    occurred_at     timestamp(6) without time zone NOT NULL,
    is_deleted      boolean,
    created_at      timestamp(6) without time zone,
    updated_at      timestamp(6) without time zone
);
CREATE INDEX IF NOT EXISTS idx_delivery_event_delivery ON delivery_event (delivery_id, occurred_at);

-- Give pre-existing packages a starting point in their history.
INSERT INTO delivery_event (id, delivery_id, event_type, to_status, public_message, actor_role, occurred_at, is_deleted, created_at, updated_at)
SELECT md5(d.id || '-created'), d.id, 'CREATED', d.status, 'Package registered', 'SYSTEM',
       COALESCE(d.created_at, now()), false, now(), now()
FROM delivery d
WHERE NOT EXISTS (SELECT 1 FROM delivery_event e WHERE e.delivery_id = d.id);

-- ---------------------------------------------------------------- audit trail
CREATE TABLE IF NOT EXISTS audit_log (
    id           varchar(36) NOT NULL PRIMARY KEY,
    actor_id     varchar(36),
    actor_email  varchar(255),
    actor_role   varchar(50),
    action       varchar(60) NOT NULL,
    entity_type  varchar(40) NOT NULL,
    entity_id    varchar(64),
    details      varchar(2000),
    occurred_at  timestamp(6) without time zone NOT NULL,
    is_deleted   boolean,
    created_at   timestamp(6) without time zone,
    updated_at   timestamp(6) without time zone
);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_log (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_occurred ON audit_log (occurred_at);

-- ---------------------------------------------------------------- reviews
CREATE TABLE IF NOT EXISTS order_review (
    id               varchar(36) NOT NULL PRIMARY KEY,
    order_id         varchar(36) NOT NULL UNIQUE REFERENCES t_orders (id),
    customer_id      varchar(36) NOT NULL REFERENCES users (id),
    rating           integer NOT NULL CHECK (rating BETWEEN 1 AND 5),
    delivery_rating  integer CHECK (delivery_rating IS NULL OR delivery_rating BETWEEN 1 AND 5),
    comment          varchar(1000),
    is_deleted       boolean,
    created_at       timestamp(6) without time zone,
    updated_at       timestamp(6) without time zone
);

-- ---------------------------------------------------------------- wallet ledger
ALTER TABLE wallet_transactions DROP CONSTRAINT IF EXISTS wallet_transactions_type_check;
ALTER TABLE wallet_transactions ADD CONSTRAINT wallet_transactions_type_check
    CHECK (type IN ('CREDIT_PENDING', 'RELEASE_TO_AVAILABLE', 'PAYOUT_DEBIT', 'REVERSE_PENDING'));
ALTER TABLE payment_splits DROP CONSTRAINT IF EXISTS payment_splits_status_check;
ALTER TABLE payment_splits ADD CONSTRAINT payment_splits_status_check
    CHECK (status IS NULL OR status IN ('PENDING', 'AVAILABLE', 'DISBURSED', 'REVERSED'));
CREATE INDEX IF NOT EXISTS idx_payment_splits_payment ON payment_splits (payment_id);

-- Driver ratings were a hardcoded 5.0 default; from now on they are computed from customer reviews only.
UPDATE driver_profile SET rating = NULL;
