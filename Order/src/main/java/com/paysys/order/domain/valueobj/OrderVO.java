package com.paysys.order.domain.valueobj;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 订单表
 * @TableName orders
 */
@TableName(value ="orders")
@Data
public class OrderVO implements Serializable {
    /**
     * 内部主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 业务订单号（UUID 或类似规则生成）
     */
    private String orderId;

    /**
     * 客户 ID
     */
    private String customerId;

    /**
     * 订单项列表（JSON 格式）
     */
    private String items;

    /**
     * 订单状态（数字表示）
     */
    private Integer status;

    /**
     * 支付链接
     */
    private String paymentLink;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 备注信息
     */
    private String note;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDeleted;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}