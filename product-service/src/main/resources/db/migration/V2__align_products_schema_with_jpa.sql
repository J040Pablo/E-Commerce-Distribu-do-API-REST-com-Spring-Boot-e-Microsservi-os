-- ============================================================
-- PRODUCT SERVICE SCHEMA ALIGNMENT
-- V2__align_products_schema_with_jpa.sql
-- ============================================================

ALTER TABLE products
    CHANGE COLUMN stock quantity INT NOT NULL;

ALTER TABLE products
    DROP COLUMN available;

ALTER TABLE products
    MODIFY COLUMN description TEXT NOT NULL,
    MODIFY COLUMN price DECIMAL(10, 2) NOT NULL,
    MODIFY COLUMN created_at DATETIME NOT NULL,
    MODIFY COLUMN updated_at DATETIME NULL;
