-- Payment Service Schema
-- Version 1.0.0 - Initial schema creation

-- Payments Table
CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'BRL' NOT NULL,
    status VARCHAR(50) NOT NULL COMMENT 'PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED',
    payment_method VARCHAR(50) NOT NULL COMMENT 'CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, PIX',
    transaction_id VARCHAR(255) UNIQUE,
    gateway_response TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    COMMENT 'Records all payment transactions for orders'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_order_id ON payments(order_id);
CREATE INDEX idx_customer_id ON payments(customer_id);
CREATE INDEX idx_status ON payments(status);
CREATE INDEX idx_transaction_id ON payments(transaction_id);
CREATE INDEX idx_created_at ON payments(created_at);
CREATE INDEX idx_updated_at ON payments(updated_at);

-- Payment Events Table
CREATE TABLE payment_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL COMMENT 'PAYMENT_INITIATED, PAYMENT_AUTHORIZED, PAYMENT_CAPTURED, PAYMENT_DECLINED, PAYMENT_CANCELLED, PAYMENT_REFUNDED',
    event_status VARCHAR(50) NOT NULL,
    event_details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    COMMENT 'Event log for payment status transitions',
    CONSTRAINT fk_payment_events_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_payment_id_event ON payment_events(payment_id);
CREATE INDEX idx_event_type ON payment_events(event_type);
CREATE INDEX idx_event_created_at ON payment_events(created_at);

-- Payment Audit Table
CREATE TABLE payment_audit (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL COMMENT 'CREATE, UPDATE, DELETE, STATUS_CHANGE',
    old_values JSON,
    new_values JSON,
    changed_by VARCHAR(100),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    COMMENT 'Audit trail for payment changes',
    CONSTRAINT fk_payment_audit_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_payment_id_audit ON payment_audit(payment_id);
CREATE INDEX idx_action ON payment_audit(action);
CREATE INDEX idx_changed_at ON payment_audit(changed_at);
