package com.paysys.order.domain.entities;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.paysys.order.domain.enums.OrderStatusEnum;
import com.paysys.vo.OrderItem;
import com.paysys.order.domain.valueobj.OrderVO;
import lombok.Data;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class Order {
    private static final Type orderItemListType = new TypeToken<List<OrderItem>>() {}.getType();

    private Long id; // 数据库自增主键
    private String orderId; // 业务订单号
    private String customerId; // 客户ID
    private List<OrderItem> items; // 订单项（JSON 格式）
    private OrderStatusEnum status; // 订单状态
    private String paymentLink; // 支付链接
    private BigDecimal totalAmount; // 总金额
    private String note; // 备注
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间

    public BigDecimal calculateTotalAmount(){
        BigDecimal totalAmount = BigDecimal.ZERO;
        for(OrderItem item : items){
            totalAmount = totalAmount.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return totalAmount;
    }

    public boolean isValid(){
        if(orderId == null || customerId == null || items == null || status == null){
            return false;
        }
        for(OrderItem item : items){
            if(item.getQuantity() <= 0){
                return false;
            }
        }
        if(totalAmount.compareTo(BigDecimal.ZERO) <= 0 || totalAmount.compareTo(calculateTotalAmount()) != 0){
            return false;
        }
        return true;
    }

    public String generateOrderId(){
        if(orderId == null){
            return UUID.randomUUID().toString();
        }
        return orderId;
    }

    public void Created(){
        this.status = OrderStatusEnum.CREATED;
    }

    public void Paid(){
        this.status = OrderStatusEnum.PAID;
    }

    public void Pending(){
        this.status = OrderStatusEnum.PENDING;
    }

    public OrderVO toVO(){
        OrderVO orderVO = new OrderVO();
        orderVO.setId(this.id);
        orderVO.setOrderId(this.orderId);
        orderVO.setCustomerId(this.customerId);
        orderVO.setItems(new Gson().toJson(this.items));
        orderVO.setStatus(this.status.getCode());
        orderVO.setPaymentLink(this.paymentLink);
        orderVO.setTotalAmount(this.totalAmount);
        orderVO.setNote(this.note);
        return orderVO;
    }

    public static Order fromVO(OrderVO orderVO){
        Order order = new Order();
        if(orderVO.getId() != null){
            order.setId(orderVO.getId());
        }
        if(orderVO.getOrderId() != null){
            order.setOrderId(orderVO.getOrderId());
        }
        if(orderVO.getCustomerId() != null){
            order.setCustomerId(orderVO.getCustomerId());
        }
        if(orderVO.getItems() != null){
            order.setItems(new Gson().fromJson(orderVO.getItems(), orderItemListType));
        }
        if(orderVO.getStatus() != null){
            order.setStatus(OrderStatusEnum.fromCode(orderVO.getStatus()));
        }
        if(orderVO.getPaymentLink() != null){
            order.setPaymentLink(orderVO.getPaymentLink());
        }
        if(orderVO.getTotalAmount() != null){
            order.setTotalAmount(orderVO.getTotalAmount());
        }
        if(orderVO.getNote() != null){
            order.setNote(orderVO.getNote());
        }
        return order;
    }
}
