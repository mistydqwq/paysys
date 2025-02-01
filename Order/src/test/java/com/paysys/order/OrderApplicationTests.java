package com.paysys.order;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.paysys.order.adapters.outbound.OrderMapper;
import com.paysys.order.domain.valueobj.OrderVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
class OrderApplicationTests {

    @Autowired
    private OrderMapper orderMapper;

    @Test
    public void testInsertAndSelect() {
        // Prepare test data
        String orderId = UUID.randomUUID().toString();
        OrderVO order = new OrderVO();
        order.setOrderId(orderId);
        order.setCustomerId("123");
        order.setItems("[{\"productId\":\"p1\",\"quantity\":1}]");
        order.setStatus(0);
        order.setTotalAmount(BigDecimal.valueOf(100.05));

        // Insert data
        int rowsInserted = orderMapper.insert(order);
        assertThat(rowsInserted).isEqualTo(1);

        // Query by order_id
        QueryWrapper<OrderVO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId);
        OrderVO fetchedOrder = orderMapper.selectOne(queryWrapper);

        // Assert data
        assertThat(fetchedOrder).isNotNull();
        assertThat(fetchedOrder.getOrderId()).isEqualTo(orderId);
        assertThat(fetchedOrder.getCustomerId()).isEqualTo("123");
        assertThat(fetchedOrder.getItems()).isEqualTo("[{\"productId\":\"p1\",\"quantity\":1}]");
    }

}
