-- Customer Service Schema
-- Version 1.0.0 - Initial schema creation

-- Customers Table
CREATE TABLE customers (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    address VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(2),
    zip_code VARCHAR(10),
    document_number VARCHAR(20) UNIQUE COMMENT 'CPF/CNPJ',
    customer_type VARCHAR(50) DEFAULT 'INDIVIDUAL' COMMENT 'INDIVIDUAL, CORPORATE',
    status VARCHAR(50) DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE, SUSPENDED, DELETED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    COMMENT 'Core customer information'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_email ON customers(email);
CREATE INDEX idx_document_number ON customers(document_number);
CREATE INDEX idx_status ON customers(status);
CREATE INDEX idx_created_at ON customers(created_at);
CREATE INDEX idx_updated_at ON customers(updated_at);
CREATE INDEX idx_name ON customers(name);

-- Customer Preferences Table
CREATE TABLE customer_preferences (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    preference_key VARCHAR(100) NOT NULL,
    preference_value VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    UNIQUE KEY unique_customer_preference (customer_id, preference_key),
    COMMENT 'Customer preferences and settings',
    CONSTRAINT fk_customer_preferences_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_customer_id_pref ON customer_preferences(customer_id);

-- Customer Addresses Table
CREATE TABLE customer_addresses (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    address_type VARCHAR(50) NOT NULL COMMENT 'BILLING, SHIPPING, OTHER',
    street VARCHAR(255) NOT NULL,
    number VARCHAR(20) NOT NULL,
    complement VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(2) NOT NULL,
    zip_code VARCHAR(10) NOT NULL,
    country VARCHAR(100) DEFAULT 'Brazil' NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    COMMENT 'Multiple addresses per customer',
    CONSTRAINT fk_customer_addresses_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_customer_id_addr ON customer_addresses(customer_id);
CREATE INDEX idx_address_type ON customer_addresses(address_type);

-- Customer Audit Table
CREATE TABLE customer_audit (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL COMMENT 'CREATE, UPDATE, DELETE, STATUS_CHANGE',
    old_values JSON,
    new_values JSON,
    changed_by VARCHAR(100),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    COMMENT 'Audit trail for customer changes',
    CONSTRAINT fk_customer_audit_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_customer_id_audit ON customer_audit(customer_id);
CREATE INDEX idx_action ON customer_audit(action);
CREATE INDEX idx_changed_at ON customer_audit(changed_at);
