-- =========================
-- ORDERS TABLE
-- =========================
CREATE TABLE orders (
                        order_id BIGSERIAL PRIMARY KEY,
                        customer_id BIGINT NOT NULL,
                        restaurant_id BIGINT NOT NULL,

                        status VARCHAR(20) NOT NULL CHECK (
                            status IN (
                                       'PLACED',
                                       'PAID',
                                       'CONFIRMED',
                                       'PREPARED',
                                       'READY',
                                       'PICKED_UP',
                                       'DELIVERED',
                                       'CANCELLED'
                                )
                            ),

                        placed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        total_amount NUMERIC(10,2) NOT NULL,

                        delivery_street VARCHAR(255),
                        delivery_city VARCHAR(100),
                        delivery_postal_code VARCHAR(20)
);

-- =========================
-- ORDER_ITEMS TABLE
-- =========================
CREATE TABLE order_items (
                             order_item_id BIGSERIAL PRIMARY KEY,
                             order_id BIGINT NOT NULL,
                             menu_item_id BIGINT NOT NULL,

                             name VARCHAR(255) NOT NULL,
                             unit_price NUMERIC(10,2) NOT NULL,
                             quantity INT NOT NULL CHECK (quantity > 0),

                             CONSTRAINT fk_order
                                 FOREIGN KEY (order_id)
                                     REFERENCES orders(order_id)
                                     ON DELETE CASCADE
);

-- =========================
-- INDEXES
-- =========================
CREATE INDEX idx_order_items_order_id
    ON order_items(order_id);