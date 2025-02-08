package com.paysys.order;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.paysys.common.BaseResponse;
import com.paysys.order.adapters.outbound.OrderMapper;
import com.paysys.order.application.commands.CreateOrderCommand;
import com.paysys.order.domain.entities.Order;
import com.paysys.order.domain.valueobj.OrderVO;
import com.paysys.order.ports.inbound.CreateOrderUseCase;
import com.paysys.order.ports.outbound.OrderRepositoryPort;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@EnableDubbo
class OrderApplicationTests {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    @Qualifier("redisRepositoryImpl")
    private OrderRepositoryPort orderRepositoryPort;

    @Test
    public void testInsertAndSelect() {
        // Prepare test data
        String orderId = UUID.randomUUID().toString();
        OrderVO order = new OrderVO();
        order.setOrderId(orderId);
        order.setCustomerId("abc");
        order.setItems("[{\"productId\":\"p1\",\"quantity\":1}]");
        order.setStatus(0);
        order.setTotalAmount(BigDecimal.valueOf(100.05));

        boolean res = orderRepositoryPort.save(Order.fromVO(order));

        boolean res1 = orderRepositoryPort.findById(orderId).isPresent();

        System.out.println(res);
        System.out.println(res1);
        // Insert data
//        int rowsInserted = orderMapper.insert(order);
//        assertThat(rowsInserted).isEqualTo(1);
//
//        // Query by order_id
//        QueryWrapper<OrderVO> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("order_id", orderId);
//        OrderVO fetchedOrder = orderMapper.selectOne(queryWrapper);
//
//        // Assert data
//        assertThat(fetchedOrder).isNotNull();
//        assertThat(fetchedOrder.getOrderId()).isEqualTo(orderId);
//        assertThat(fetchedOrder.getCustomerId()).isEqualTo("123");
//        assertThat(fetchedOrder.getItems()).isEqualTo("[{\"productId\":\"p1\",\"quantity\":1}]");
    }

    @Test
    public void testCreateOrder()throws InterruptedException {
        CreateOrderCommand command = new CreateOrderCommand();
        command.setCustomerId("tyn");
        command.setItems("[{\"productId\":\"p1\",\"quantity\":1,\"price\":100.05}]");
        command.setNote("test");
        BaseResponse<String> response = createOrderUseCase.createOrder(command);
        Thread.sleep(30_000);
    }

}
