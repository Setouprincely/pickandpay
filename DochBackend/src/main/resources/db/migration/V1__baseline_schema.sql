-- V1: Baseline schema of the DOCH platform as it existed before Flyway was introduced.
-- Generated from the JPA entity model (Hibernate DDL) on PostgreSQL. Existing databases are baselined at this version.
CREATE TABLE public.category (
    is_deleted boolean,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    description character varying(255),
    id character varying(36) NOT NULL,
    name character varying(255)
);

CREATE TABLE public.delivery (
    is_deleted boolean,
    created_at timestamp(6) without time zone,
    end_time timestamp(6) without time zone,
    estimated_delivery_time timestamp(6) without time zone,
    start_time timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    delivery_otp character varying(255),
    driver_id character varying(36),
    id character varying(36) NOT NULL,
    live_location_id character varying(36),
    market_request_id character varying(36),
    pod_image_id character varying(36),
    status character varying(255),
    xender_request_id character varying(36),
    CONSTRAINT delivery_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING_PAYMENT'::character varying, 'PENDING_PREPARATION'::character varying, 'PENDING_ASSIGNMENT'::character varying, 'DRIVER_ASSIGNED'::character varying, 'PICKED_UP'::character varying, 'IN_TRANSIT'::character varying, 'DELIVERED'::character varying, 'CANCELLED'::character varying, 'FAILED'::character varying])::text[])))
);

CREATE TABLE public.delivery_image (
    is_deleted boolean,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    content_type character varying(255),
    id character varying(36) NOT NULL,
    image_url character varying(255),
    xender_request_id character varying(36)
);

CREATE TABLE public.doch_market_delivery_request (
    is_deleted boolean,
    price double precision,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    delivery_note character varying(255),
    id character varying(36) NOT NULL,
    order_id character varying(36),
    pickup_location_id character varying(36),
    receiver_location_id character varying(36),
    sealed_image_id character varying(36),
    seller_id character varying(36)
);

CREATE TABLE public.doch_xender_delivery_request (
    is_deleted boolean,
    price double precision,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    delivery_note character varying(255),
    delivery_type character varying(255),
    id character varying(36) NOT NULL,
    pickup_location_address character varying(255),
    pickup_location_id character varying(36),
    receiver_address character varying(255),
    receiver_location_id character varying(36),
    sender_id character varying(36),
    status character varying(255),
    transport_type character varying(255),
    CONSTRAINT doch_xender_delivery_request_transport_type_check CHECK (((transport_type)::text = ANY ((ARRAY['SINGLE_PARCEL'::character varying, 'FULL_TRUCK'::character varying])::text[])))
);

CREATE TABLE public.driver_profile (
    is_deleted boolean,
    rating double precision,
    total_deliveries integer,
    created_at timestamp(6) without time zone,
    last_location_update timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id character varying(36) NOT NULL,
    last_latitude character varying(255),
    last_longitude character varying(255),
    license_number character varying(255),
    plate_number character varying(255),
    user_id character varying(36) NOT NULL,
    vehicle_type character varying(255)
);

CREATE TABLE public.fleet_vehicle (
    is_deleted boolean,
    last_maintenance_date date,
    next_maintenance_date date,
    year integer,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    notes character varying(1000),
    assigned_driver_profile_id character varying(36),
    id character varying(36) NOT NULL,
    make character varying(255),
    model character varying(255),
    name character varying(255),
    plate_number character varying(255),
    status character varying(255),
    vehicle_type character varying(255),
    CONSTRAINT fleet_vehicle_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'IN_MAINTENANCE'::character varying, 'INACTIVE'::character varying, 'RETIRED'::character varying])::text[])))
);

CREATE TABLE public.location (
    is_deleted boolean,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id character varying(36) NOT NULL,
    latitude character varying(255),
    longitude character varying(255)
);

CREATE TABLE public.notification (
    is_deleted boolean,
    created_at timestamp(6) without time zone,
    sent_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id character varying(36) NOT NULL,
    message character varying(255),
    type character varying(255),
    CONSTRAINT notification_type_check CHECK (((type)::text = ANY ((ARRAY['WELCOME'::character varying, 'ACCOUNT_VERIFIED'::character varying, 'PASSWORD_RESET'::character varying, 'ORDER_PLACED'::character varying, 'ORDER_CONFIRMED'::character varying, 'ORDER_SHIPPED'::character varying, 'ORDER_DELIVERED'::character varying, 'ORDER_CANCELLED'::character varying, 'DELIVERY_REQUEST_RECEIVED'::character varying, 'DELIVERY_DRIVER_ASSIGNED'::character varying, 'DELIVERY_IN_TRANSIT'::character varying, 'DELIVERY_PICKED_UP'::character varying, 'DELIVERY_COMPLETED'::character varying, 'DELIVERY_FAILED'::character varying, 'PAYMENT_SUCCESS'::character varying, 'PAYMENT_FAILED'::character varying, 'REFUND_PROCESSED'::character varying, 'SYSTEM_ANNOUNCEMENT'::character varying, 'PROMOTION'::character varying])::text[])))
);

CREATE TABLE public.order_item (
    is_deleted boolean,
    quantity integer NOT NULL,
    total_price double precision NOT NULL,
    unite_priced double precision NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id character varying(36) NOT NULL,
    order_id character varying(36),
    product_id character varying(36)
);

CREATE TABLE public.payment (
    amount double precision NOT NULL,
    is_deleted boolean,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    payment_url character varying(500),
    gateway_transaction_id character varying(255),
    id character varying(36) NOT NULL,
    order_id character varying(36),
    payment_method character varying(255),
    status character varying(255),
    user_id character varying(36) NOT NULL,
    xender_request_id character varying(36),
    CONSTRAINT payment_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PROCESSING'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying, 'FAILED'::character varying])::text[])))
);

CREATE TABLE public.payment_splits (
    amount double precision,
    is_deleted boolean,
    created_at timestamp(6) without time zone,
    eligible_payout_date timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id character varying(36) NOT NULL,
    payment_id character varying(36),
    status character varying(255),
    type character varying(255),
    user_id character varying(36),
    CONSTRAINT payment_splits_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'AVAILABLE'::character varying, 'DISBURSED'::character varying])::text[]))),
    CONSTRAINT payment_splits_type_check CHECK (((type)::text = ANY ((ARRAY['SELLER_EARNING'::character varying, 'DELIVERY_FEE'::character varying, 'PLATFORM_COMMISSION'::character varying])::text[])))
);

CREATE TABLE public.permissions (
    is_deleted boolean,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    description character varying(255),
    id character varying(36) NOT NULL,
    name character varying(255)
);

CREATE TABLE public.pricing_settings (
    base_price double precision,
    is_deleted boolean,
    min_price double precision,
    peak_surcharge_enabled boolean,
    peak_surcharge_percent double precision,
    price_per_km double precision,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id character varying(36) NOT NULL
);

CREATE TABLE public.product (
    is_activity boolean NOT NULL,
    is_deleted boolean,
    price double precision NOT NULL,
    stock_quantity integer NOT NULL,
    created_at timestamp(6) without time zone,
    quantity_sold bigint DEFAULT 0,
    updated_at timestamp(6) without time zone,
    currency character varying(255),
    description character varying(255),
    id character varying(36) NOT NULL,
    name character varying(255),
    seller_id character varying(36),
    sku character varying(255),
    weight character varying(255)
);

CREATE TABLE public.product_category (
    category_id character varying(36) NOT NULL,
    product_id character varying(36) NOT NULL
);

CREATE TABLE public.product_image (
    is_deleted boolean,
    is_primary boolean NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    content_type character varying(255),
    id character varying(36) NOT NULL,
    image_url character varying(255),
    names character varying(255),
    product_id character varying(36)
);

CREATE TABLE public.role_permissions (
    permission_id character varying(36) NOT NULL,
    role_id character varying(36) NOT NULL
);

CREATE TABLE public.roles (
    is_deleted boolean,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id character varying(36) NOT NULL,
    name character varying(255)
);

CREATE TABLE public.seller (
    is_deleted boolean,
    is_verified boolean,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    address character varying(255),
    city character varying(255),
    contact_number character varying(255),
    id character varying(36) NOT NULL,
    name character varying(255),
    rating character varying(255),
    user_id character varying(36)
);

CREATE TABLE public.support_report (
    is_deleted boolean,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    resolution_note character varying(1000),
    description character varying(2000),
    category character varying(255),
    delivery_id character varying(255),
    id character varying(36) NOT NULL,
    reporter_id character varying(36) NOT NULL,
    status character varying(255),
    subject character varying(255),
    CONSTRAINT support_report_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'IN_REVIEW'::character varying, 'RESOLVED'::character varying, 'DISMISSED'::character varying])::text[])))
);

CREATE TABLE public.t_orders (
    delivery_fee double precision NOT NULL,
    is_deleted boolean,
    product_total double precision NOT NULL,
    tax_amount double precision NOT NULL,
    total_amount double precision NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    delivery_address character varying(500),
    customer_id character varying(36),
    discount_code character varying(255),
    id character varying(36) NOT NULL,
    status character varying(255),
    transport_type character varying(255),
    CONSTRAINT t_orders_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PROCESSING'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying, 'SHIPPED'::character varying, 'IN_TRANSIT'::character varying, 'DELIVERED'::character varying, 'RETURNED'::character varying, 'REFUNDED'::character varying])::text[]))),
    CONSTRAINT t_orders_transport_type_check CHECK (((transport_type)::text = ANY ((ARRAY['SINGLE_PARCEL'::character varying, 'FULL_TRUCK'::character varying])::text[])))
);

CREATE TABLE public.token (
    id integer NOT NULL,
    is_logged_out boolean,
    access_token text,
    refresh_token text,
    user_id character varying(36) NOT NULL
);

ALTER TABLE public.token ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.token_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.user_notifications (
    is_read boolean NOT NULL,
    clicked_at timestamp(6) without time zone,
    id character varying(36) NOT NULL,
    notification_id character varying(36) NOT NULL,
    user_id character varying(36) NOT NULL
);

CREATE TABLE public.userprofile (
    last_login timestamp(6) without time zone,
    bio character varying(255),
    gender character varying(255),
    id character varying(36) NOT NULL,
    profile_picture character varying(255)
);

CREATE TABLE public.users (
    enabled boolean,
    is_available boolean NOT NULL,
    is_deleted boolean,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    email character varying(255),
    first_name character varying(255),
    id character varying(36) NOT NULL,
    language character varying(255),
    last_name character varying(255),
    password character varying(255),
    tel character varying(255),
    user_name character varying(255),
    user_role_id character varying(36)
);

CREATE TABLE public.verification_token (
    is_deleted boolean,
    is_used boolean NOT NULL,
    created_at timestamp(6) without time zone,
    created_time timestamp(6) without time zone,
    expires_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    c_user_id character varying(36) NOT NULL,
    id character varying(36) NOT NULL,
    otp_code character varying(255)
);

CREATE TABLE public.wallet_transactions (
    amount double precision,
    is_deleted boolean,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    description character varying(255),
    id character varying(255) NOT NULL,
    type character varying(255),
    wallet_id character varying(255),
    CONSTRAINT wallet_transactions_type_check CHECK (((type)::text = ANY ((ARRAY['CREDIT_PENDING'::character varying, 'RELEASE_TO_AVAILABLE'::character varying, 'PAYOUT_DEBIT'::character varying])::text[])))
);

CREATE TABLE public.wallets (
    available_balance double precision,
    is_deleted boolean,
    pending_balance double precision,
    total_withdrawn double precision,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id character varying(255) NOT NULL,
    user_id character varying(36) NOT NULL
);

ALTER TABLE ONLY public.category
    ADD CONSTRAINT category_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.delivery_image
    ADD CONSTRAINT delivery_image_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.delivery
    ADD CONSTRAINT delivery_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.delivery
    ADD CONSTRAINT delivery_pod_image_id_key UNIQUE (pod_image_id);

ALTER TABLE ONLY public.doch_market_delivery_request
    ADD CONSTRAINT doch_market_delivery_request_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.doch_market_delivery_request
    ADD CONSTRAINT doch_market_delivery_request_sealed_image_id_key UNIQUE (sealed_image_id);

ALTER TABLE ONLY public.doch_xender_delivery_request
    ADD CONSTRAINT doch_xender_delivery_request_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.driver_profile
    ADD CONSTRAINT driver_profile_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.driver_profile
    ADD CONSTRAINT driver_profile_user_id_key UNIQUE (user_id);

ALTER TABLE ONLY public.fleet_vehicle
    ADD CONSTRAINT fleet_vehicle_assigned_driver_profile_id_key UNIQUE (assigned_driver_profile_id);

ALTER TABLE ONLY public.fleet_vehicle
    ADD CONSTRAINT fleet_vehicle_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.fleet_vehicle
    ADD CONSTRAINT fleet_vehicle_plate_number_key UNIQUE (plate_number);

ALTER TABLE ONLY public.location
    ADD CONSTRAINT location_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT notification_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.order_item
    ADD CONSTRAINT order_item_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT payment_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.payment_splits
    ADD CONSTRAINT payment_splits_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_name_key UNIQUE (name);

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.pricing_settings
    ADD CONSTRAINT pricing_settings_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.product_image
    ADD CONSTRAINT product_image_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.product
    ADD CONSTRAINT product_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_name_key UNIQUE (name);

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.seller
    ADD CONSTRAINT seller_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.support_report
    ADD CONSTRAINT support_report_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.t_orders
    ADD CONSTRAINT t_orders_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.token
    ADD CONSTRAINT token_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.user_notifications
    ADD CONSTRAINT user_notifications_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.userprofile
    ADD CONSTRAINT userprofile_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.verification_token
    ADD CONSTRAINT verification_token_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.wallet_transactions
    ADD CONSTRAINT wallet_transactions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.wallets
    ADD CONSTRAINT wallets_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.wallets
    ADD CONSTRAINT wallets_user_id_key UNIQUE (user_id);

ALTER TABLE ONLY public.product_category
    ADD CONSTRAINT fk2k3smhbruedlcrvu6clued06x FOREIGN KEY (product_id) REFERENCES public.product(id);

ALTER TABLE ONLY public.doch_market_delivery_request
    ADD CONSTRAINT fk2mdo3jeciminhqgiaaejy892h FOREIGN KEY (receiver_location_id) REFERENCES public.location(id);

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT fk4lvnmj3xiao171ixowurgjhqd FOREIGN KEY (xender_request_id) REFERENCES public.doch_xender_delivery_request(id);

ALTER TABLE ONLY public.order_item
    ADD CONSTRAINT fk551losx9j75ss5d6bfsqvijna FOREIGN KEY (product_id) REFERENCES public.product(id);

ALTER TABLE ONLY public.t_orders
    ADD CONSTRAINT fk5imvb8o1h50wh43n6joajtsxy FOREIGN KEY (customer_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.seller
    ADD CONSTRAINT fk614u1eblpnxmrxd25efo29qhr FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.driver_profile
    ADD CONSTRAINT fk6cohe1xdv0wo0ell93qmyj33r FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.payment_splits
    ADD CONSTRAINT fk6d37vtoahk1epjovu3nxvnji2 FOREIGN KEY (payment_id) REFERENCES public.payment(id);

ALTER TABLE ONLY public.product_image
    ADD CONSTRAINT fk6oo0cvcdtb6qmwsga468uuukk FOREIGN KEY (product_id) REFERENCES public.product(id);

ALTER TABLE ONLY public.doch_market_delivery_request
    ADD CONSTRAINT fk72aospddf907a05nla23auysr FOREIGN KEY (seller_id) REFERENCES public.seller(id);

ALTER TABLE ONLY public.wallet_transactions
    ADD CONSTRAINT fk8seu7b87ifqi09ghhssusmb0x FOREIGN KEY (wallet_id) REFERENCES public.wallets(id);

ALTER TABLE ONLY public.user_notifications
    ADD CONSTRAINT fk950xf8lkgfw6yvd0i9ijjvvng FOREIGN KEY (notification_id) REFERENCES public.notification(id);

ALTER TABLE ONLY public.user_notifications
    ADD CONSTRAINT fk9f86wonnl11hos1cuf5fibutl FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.doch_market_delivery_request
    ADD CONSTRAINT fka7isr3ejhhwpqkipj3em5ppeg FOREIGN KEY (pickup_location_id) REFERENCES public.location(id);

ALTER TABLE ONLY public.verification_token
    ADD CONSTRAINT fkacxk4focyock1034dtu5mxott FOREIGN KEY (c_user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.wallets
    ADD CONSTRAINT fkc1foyisidw7wqqrkamafuwn4e FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.delivery
    ADD CONSTRAINT fkdgbxo8c5g79ytupspji0h3p0r FOREIGN KEY (driver_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.delivery
    ADD CONSTRAINT fkdnj68ah7erlw4pcwo2o0e127q FOREIGN KEY (market_request_id) REFERENCES public.doch_market_delivery_request(id);

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT fkegdk29eiy7mdtefy5c7eirr6e FOREIGN KEY (permission_id) REFERENCES public.permissions(id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fkeiotsurvwt38o0qs1w3kj32c9 FOREIGN KEY (user_role_id) REFERENCES public.roles(id);

ALTER TABLE ONLY public.product
    ADD CONSTRAINT fkesd6fy52tk7esoo2gcls4lfe3 FOREIGN KEY (seller_id) REFERENCES public.seller(id);

ALTER TABLE ONLY public.order_item
    ADD CONSTRAINT fkf7m65eyhbs7dt3es8f1q7lam5 FOREIGN KEY (order_id) REFERENCES public.t_orders(id);

ALTER TABLE ONLY public.doch_market_delivery_request
    ADD CONSTRAINT fkfej8pu03cu329bn2tdtfgr7yp FOREIGN KEY (sealed_image_id) REFERENCES public.delivery_image(id);

ALTER TABLE ONLY public.delivery
    ADD CONSTRAINT fkfu9i6hg1we3n8we35axdlkjgu FOREIGN KEY (pod_image_id) REFERENCES public.delivery_image(id);

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT fkga13lvg20aqmdr1usx5ugq9j3 FOREIGN KEY (order_id) REFERENCES public.t_orders(id);

ALTER TABLE ONLY public.payment_splits
    ADD CONSTRAINT fkh569nq68lossjwqw2srwu218c FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.token
    ADD CONSTRAINT fkj8rfw4x0wjjyibfqq566j4qng FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.doch_xender_delivery_request
    ADD CONSTRAINT fkjivp0ymbyargtjj2eirjf0drb FOREIGN KEY (pickup_location_id) REFERENCES public.location(id);

ALTER TABLE ONLY public.product_category
    ADD CONSTRAINT fkkud35ls1d40wpjb5htpp14q4e FOREIGN KEY (category_id) REFERENCES public.category(id);

ALTER TABLE ONLY public.support_report
    ADD CONSTRAINT fklg6st8tu4lhqup3m0klsfrl7x FOREIGN KEY (reporter_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.delivery_image
    ADD CONSTRAINT fklxfus73ohkdtv682b9rlkfgu1 FOREIGN KEY (xender_request_id) REFERENCES public.doch_xender_delivery_request(id);

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT fkmi2669nkjesvp7cd257fptl6f FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT fkn5fotdgk8d1xvo8nav9uv3muc FOREIGN KEY (role_id) REFERENCES public.roles(id);

ALTER TABLE ONLY public.doch_xender_delivery_request
    ADD CONSTRAINT fkne60l16c7ucn74f3arnat917i FOREIGN KEY (sender_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.fleet_vehicle
    ADD CONSTRAINT fko45077pra2mpoqtamyjjaihyi FOREIGN KEY (assigned_driver_profile_id) REFERENCES public.driver_profile(id);

ALTER TABLE ONLY public.delivery
    ADD CONSTRAINT fkp73n9ir9b4mvms98aw6blhn8u FOREIGN KEY (xender_request_id) REFERENCES public.doch_xender_delivery_request(id);

ALTER TABLE ONLY public.doch_xender_delivery_request
    ADD CONSTRAINT fkpf43netpdw2njh4c6xh6f9gkk FOREIGN KEY (receiver_location_id) REFERENCES public.location(id);

ALTER TABLE ONLY public.delivery
    ADD CONSTRAINT fkslwf5odd2n1ak7eopn9fmvufl FOREIGN KEY (live_location_id) REFERENCES public.location(id);

ALTER TABLE ONLY public.doch_market_delivery_request
    ADD CONSTRAINT fkti7a8mgvm85k4pkj41nb6uhuq FOREIGN KEY (order_id) REFERENCES public.t_orders(id);

