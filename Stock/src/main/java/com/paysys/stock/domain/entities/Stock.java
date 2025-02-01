package com.paysys.stock.domain.entities;

import com.paysys.stock.domain.valueobj.StockVO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Stock {
    private Long id; // 数据库自增主键
    private String productId;  // 商品ID
    private String productName;  // 商品名称
    private BigDecimal price;  // 商品价格
    private Long quantity;  // 总库存数量
    private Long reservedQuantity;  // 预留库存数量
    private LocalDateTime createdAt;  // 创建时间
    private LocalDateTime updatedAt;  // 更新时间

    // 获取可用库存数量
    @JsonIgnore
    public Long getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    // 判断库存是否有效
    @JsonIgnore
    public boolean isValid() {
        if (productId == null || productId.isEmpty() || productName == null) {
            return false;
        }
        if (quantity == null || quantity < 0) {
            return false;
        }
        if (reservedQuantity == null || reservedQuantity < 0) {
            return false;
        }
        return true;
    }

    public StockVO toVO() {
        StockVO stockVO = new StockVO();
        stockVO.setId(this.id);
        stockVO.setProductId(this.productId);
        stockVO.setProductName(this.productName);
        stockVO.setQuantity(this.quantity);
        stockVO.setReservedQuantity(this.reservedQuantity);
        stockVO.setPrice(this.price);
        return stockVO;
    }

    public static Stock fromVO(StockVO vo) {
        Stock stock = new Stock();
        stock.setId(vo.getId());
        stock.setProductId(vo.getProductId());
        stock.setProductName(vo.getProductName());
        stock.setQuantity(vo.getQuantity());
        stock.setReservedQuantity(vo.getReservedQuantity());
        stock.setPrice(vo.getPrice());
        return stock;
    }
}