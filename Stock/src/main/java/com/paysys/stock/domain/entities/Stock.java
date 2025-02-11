package com.paysys.stock.domain.entities;

import com.paysys.stock.domain.valueobj.StockVO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Stock {
    private Long id;              // 数据库自增主键
    private String productId;     // 商品ID
    private String productName;   // 商品名称
    private BigDecimal price;     // 商品价格
    private Long quantity;        // 总库存数量
    private Long reservedQuantity;// 预留库存数量
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

    /**
     * 从Redis的Map中构造Stock对象
     */
    public static Stock fromRedisMap(Map<Object, Object> map) {
        Stock stock = new Stock();

        if (map.containsKey("id")) {
            stock.setId(Long.parseLong(map.get("id").toString()));
        }
        if (map.containsKey("productId")) {
            stock.setProductId(map.get("productId").toString());
        }
        if (map.containsKey("productName")) {
            stock.setProductName(map.get("productName").toString());
        }
        if (map.containsKey("price")) {
            stock.setPrice(new BigDecimal(map.get("price").toString()));
        }
        if (map.containsKey("quantity")) {
            stock.setQuantity(Long.parseLong(map.get("quantity").toString()));
        }
        if (map.containsKey("reservedQuantity")) {
            stock.setReservedQuantity(Long.parseLong(map.get("reservedQuantity").toString()));
        }
        if (map.containsKey("createdAt")) {
            stock.setCreatedAt(LocalDateTime.parse(map.get("createdAt").toString()));
        }
        if (map.containsKey("updatedAt")) {
            stock.setUpdatedAt(LocalDateTime.parse(map.get("updatedAt").toString()));
        }

        return stock;
    }

    /**
     * 转换Stock对象为适合Redis存储的Map
     */
    public Map<String, Object> toRedisMap() {
        Map<String, Object> map = new HashMap<>();

        if (this.id != null) {
            map.put("id", this.id.toString());
        }
        if (this.productId != null) {
            map.put("productId", this.productId);
        }
        if (this.productName != null) {
            map.put("productName", this.productName);
        }
        if (this.price != null) {
            map.put("price", this.price.toString());
        }
        if (this.quantity != null) {
            map.put("quantity", this.quantity.toString());
        }
        if (this.reservedQuantity != null) {
            map.put("reservedQuantity", this.reservedQuantity.toString());
        }
        if (this.createdAt != null) {
            map.put("createdAt", this.createdAt.toString());
        }
        if (this.updatedAt != null) {
            map.put("updatedAt", this.updatedAt.toString());
        }

        return map;
    }
}