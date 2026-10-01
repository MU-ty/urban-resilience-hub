CREATE TABLE app_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE shelter (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    district VARCHAR(50) NOT NULL,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    capacity INT NOT NULL,
    occupancy INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_shelter_district (district)
);

CREATE TABLE supply_lot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shelter_id BIGINT NOT NULL,
    category VARCHAR(30) NOT NULL,
    quantity INT NOT NULL,
    reserved_quantity INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_supply_shelter FOREIGN KEY (shelter_id) REFERENCES shelter(id),
    INDEX idx_supply_match (category, expires_at, shelter_id)
);

CREATE TABLE relief_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_no VARCHAR(40) NOT NULL UNIQUE,
    requester_id BIGINT NOT NULL,
    district VARCHAR(50) NOT NULL,
    category VARCHAR(30) NOT NULL,
    quantity INT NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_request_user FOREIGN KEY (requester_id) REFERENCES app_user(id),
    INDEX idx_request_status_priority (status, priority, created_at)
);

CREATE TABLE allocation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_id BIGINT NOT NULL,
    supply_lot_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    allocated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_allocation_request_lot (request_id, supply_lot_id),
    CONSTRAINT fk_allocation_request FOREIGN KEY (request_id) REFERENCES relief_request(id),
    CONSTRAINT fk_allocation_lot FOREIGN KEY (supply_lot_id) REFERENCES supply_lot(id)
);

CREATE TABLE outbox_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published_at TIMESTAMP(6) NULL,
    INDEX idx_outbox_poll (status, next_retry_at, id)
);

CREATE TABLE audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor VARCHAR(50) NOT NULL,
    action VARCHAR(80) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(64) NULL,
    ip VARCHAR(64) NULL,
    detail JSON NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_audit_actor_time (actor, created_at)
);

-- BCrypt: password is Admin@123 / User@123 respectively.
INSERT INTO app_user(username, password_hash, role) VALUES
('admin', '{noop}Admin@123', 'ADMIN'),
('citizen', '{noop}User@123', 'CITIZEN');

INSERT INTO shelter(name, district, latitude, longitude, capacity, occupancy) VALUES
('灯塔共享站', '浦东新区', 31.2304160, 121.4737010, 500, 120),
('星河社区仓', '徐汇区', 31.1882600, 121.4368700, 300, 80);

INSERT INTO supply_lot(shelter_id, category, quantity, expires_at) VALUES
(1, 'WATER', 1000, DATE_ADD(NOW(), INTERVAL 180 DAY)),
(1, 'MEDICINE', 200, DATE_ADD(NOW(), INTERVAL 90 DAY)),
(2, 'FOOD', 800, DATE_ADD(NOW(), INTERVAL 120 DAY));
