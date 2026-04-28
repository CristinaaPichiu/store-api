INSERT INTO products (title, available, price)
SELECT 'Nail gun', 8, 23.95
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE title = 'Nail gun');

INSERT INTO products (title, available, price)
SELECT 'Hammer', 15, 12.50
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE title = 'Hammer');

INSERT INTO products (title, available, price)
SELECT 'Screwdriver set', 20, 34.99
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE title = 'Screwdriver set');

INSERT INTO products (title, available, price)
SELECT 'Power drill', 5, 89.95
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE title = 'Power drill');