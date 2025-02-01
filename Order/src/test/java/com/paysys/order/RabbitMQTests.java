package com.paysys.order;

import com.paysys.order.adapters.inbound.RabbitMQConsumer;
import com.paysys.order.adapters.outbound.RabbitMQPublisher;
import com.paysys.order.domain.events.OrderCreateEvent;
import com.paysys.vo.OrderItem;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
class RabbitMQTests {

    @Autowired
    private RabbitMQPublisher rabbitMQPublisher;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private RabbitMQConsumer rabbitMQConsumer;

    @Test
    void testPublishAndConsume() throws InterruptedException {
        // 1. 定义一个测试消息
        String testOrderId = "TestOrder123";

        List<OrderItem> items = List.of(new OrderItem("p1", "Product 1", 1L, BigDecimal.valueOf(100.50)));
        OrderCreateEvent orderCreateEvent = new OrderCreateEvent(testOrderId, items, "Test note");

        // 2. 发布消息
        boolean isPublished = rabbitMQPublisher.publishEvent(orderCreateEvent);

        // 验证发布是否成功
        assertThat(isPublished).isTrue();

        // 3. 等待消费者处理消息 (可使用 Thread.sleep 简单模拟)
        //Thread.sleep(5000);

        // 4. 手动从队列中取出消息验证
        //String receivedMessage = (String) rabbitTemplate.receiveAndConvert(RabbitMQConfig.ORDER_QUEUE);

        //assertThat(receivedMessage).isNotNull();

        // 打印日志以便观察
        //System.out.println("Received message: " + receivedMessage);
    }

}
