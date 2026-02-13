-- Script SQL para inserir dados de teste na tabela de produtos
-- Este arquivo será executado automaticamente ao inicializar a aplicação

-- Insere dados de exemplo na tabela de produtos
INSERT INTO products (name, description, price, quantity, sku, category, created_at, updated_at) VALUES
('Notebook Dell XPS 13', 'Notebook de alta performance com processador Intel i7', 5999.99, 10, 'SKU001', 'Eletrônicos', NOW(), NOW()),
('Smartphone Samsung Galaxy S21', 'Smartphone com câmera 64MP e bateria 4000mAh', 3499.99, 25, 'SKU002', 'Eletrônicos', NOW(), NOW()),
('Fone Bluetooth JBL', 'Fone de ouvido wireless com cancelamento de ruído', 599.99, 50, 'SKU003', 'Acessórios', NOW(), NOW()),
('Teclado Mecânico RGB', 'Teclado gaming com switches mecânicos Cherry MX', 799.99, 15, 'SKU004', 'Periféricos', NOW(), NOW()),
('Monitor LG 27 polegadas', 'Monitor 4K com taxa de atualização 144Hz', 2299.99, 8, 'SKU005', 'Monitores', NOW(), NOW()),
('Webcam Logitech 1080p', 'Câmera web com auto-foco e microfone integrado', 399.99, 30, 'SKU006', 'Acessórios', NOW(), NOW()),
('Mouse Razer DeathAdder', 'Mouse gamer com sensor de 16000 DPI', 299.99, 20, 'SKU007', 'Periféricos', NOW(), NOW()),
('Mousepad XL Extended', 'Mousepad grande com base antiderrapante', 149.99, 40, 'SKU008', 'Acessórios', NOW(), NOW());
