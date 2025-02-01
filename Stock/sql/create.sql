CREATE DATABASE IF NOT EXISTS payment;
USE payment;

DROP TABLE IF EXISTS `stocks`;

CREATE TABLE IF NOT EXISTS stocks
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '内部主键',
    product_id      VARCHAR(36) NOT NULL UNIQUE COMMENT '商品ID',
    product_name    VARCHAR(255) NOT NULL COMMENT '商品名称',
    price           DECIMAL(10,2) NOT NULL COMMENT '商品价格',
    quantity        BIGINT NOT NULL CHECK (quantity >= 0) COMMENT '总库存数量',
    reserved_quantity BIGINT NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0) COMMENT '预留库存数量',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1)) COMMENT '是否删除(0:未删除, 1:已删除)',

    UNIQUE KEY idx_product_id (product_id) COMMENT '商品唯一索引',
    INDEX idx_name (product_name) COMMENT '商品名称索引',

    -- 确保各个数量之间的关系正确
    CONSTRAINT chk_quantity CHECK (quantity >= reserved_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '库存表' ;

DROP TABLE IF EXISTS `stock_transactions`;


CREATE TABLE IF NOT EXISTS stock_transactions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '内部主键',
    order_id        VARCHAR(36) NOT NULL COMMENT '订单ID',
    product_id      VARCHAR(36) NOT NULL COMMENT '商品ID',
    operation_type  VARCHAR(20) NOT NULL COMMENT '操作类型(RESERVE/RELEASE)',
    quantity        BIGINT NOT NULL COMMENT '操作数量',
    unit_price      DECIMAL(10,2) NOT NULL COMMENT '单价',
    total_amount    DECIMAL(10,2) NOT NULL COMMENT '总金额',
    status          VARCHAR(20) NOT NULL COMMENT '操作状态(SUCCESS/FAILED)',
    error_message   VARCHAR(255) DEFAULT NULL COMMENT '错误信息',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_order_id (order_id) COMMENT '订单ID索引',
    INDEX idx_product_id (product_id) COMMENT '商品ID索引',
    INDEX idx_created_at (created_at) COMMENT '创建时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '库存交易记录表';