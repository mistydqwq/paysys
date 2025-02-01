CREATE TABLE orders
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '内部主键',
    order_id    VARCHAR(36) NOT NULL UNIQUE COMMENT '业务订单号（UUID 或类似规则生成）',
    customer_id VARCHAR(36) NOT NULL COMMENT '客户 ID',
    items       TEXT NOT NULL COMMENT '订单项列表（JSON 格式）',
    status      TINYINT NOT NULL COMMENT '订单状态（数字表示）',
    payment_link VARCHAR(255) DEFAULT NULL COMMENT '支付链接',
    total_amount DECIMAL(10, 2) NOT NULL COMMENT '订单总金额',
    note        VARCHAR(500) DEFAULT NULL COMMENT '备注信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted  TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    UNIQUE KEY idx_order_id (order_id),
    INDEX idx_customer_id (customer_id)
) COMMENT '订单表' COLLATE = utf8mb4_unicode_ci;