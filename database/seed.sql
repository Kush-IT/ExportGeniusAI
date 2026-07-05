-- Seed roles
INSERT INTO roles (id, name) VALUES 
(1, 'EXPORTER'),
(2, 'ADMIN'),
(3, 'IMPORTER')
ON CONFLICT (id) DO NOTHING;

-- Seed categories
INSERT INTO categories (id, name) VALUES
(1, 'Textiles'),
(2, 'Electronics'),
(3, 'Spices'),
(4, 'Handicrafts'),
(5, 'Chemicals')
ON CONFLICT (id) DO NOTHING;
