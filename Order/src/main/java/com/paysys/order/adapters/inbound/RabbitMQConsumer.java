package com.paysys.order.adapters.inbound;

import com.google.gson.Gson;
import com.paysys.order.domain.events.OrderCreateEvent;
import com.paysys.order.domain.events.UpdateOrderStatusEvent;
import com.paysys.order.domain.events.UpdatePaymentLinkEvent;
import com.paysys.order.infrastructure.RabbitMQConfig;
import com.paysys.order.ports.inbound.UpdateOrderStatusUseCase;
import com.paysys.order.ports.inbound.UpdatePaymentLinkUseCase;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQConsumer {
    private final Gson gson = new Gson(); // 初始化 Gson 实例
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final UpdatePaymentLinkUseCase updatePaymentLinkUseCase;

    public RabbitMQConsumer(UpdateOrderStatusUseCase updateOrderStatusUseCase, UpdatePaymentLinkUseCase updatePaymentLinkUseCase) {
        this.updateOrderStatusUseCase = updateOrderStatusUseCase;
        this.updatePaymentLinkUseCase = updatePaymentLinkUseCase;
    }

//    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
//    public void handleOrderCreatedEvent(String message) {
//        try {
//            // 使用 Gson 将 JSON 字符串反序列化为 OrderCreateEvent 对象
//            OrderCreateEvent event = gson.fromJson(message, OrderCreateEvent.class);
//            System.out.println("Received event: " + event);
//            // 处理事件逻辑
//        } catch (Exception e) {
//            System.err.println("Failed to process message: " + e.getMessage());
//        }
//    }

    @RabbitListener(queues = RabbitMQConfig.UPDATE_ORDER_STATUS_QUEUE)
    public void handleUpdateStatusEvent(String message) {
        try {
            // 使用 Gson 将 JSON 字符串反序列化为 OrderCreateEvent 对象
            UpdateOrderStatusEvent event = gson.fromJson(message, UpdateOrderStatusEvent.class);
            System.out.println("Received event: " + event);
            // 处理事件逻辑
            updateOrderStatusUseCase.updateOrderStatus(event.getOrderId(), event.getStatus());
        } catch (Exception e) {
            System.err.println("Failed to process message: " + e.getMessage());
        }
    }

    @RabbitListener(queues = RabbitMQConfig.UPDATE_PAYMENT_LINK_QUEUE)
    public void handleUpdatePaymentLinkEvent(String message) {
        try {
            // 使用 Gson 将 JSON 字符串反序列化为 OrderCreateEvent 对象
            UpdatePaymentLinkEvent event = gson.fromJson(message, UpdatePaymentLinkEvent.class);
            updatePaymentLinkUseCase.updatePaymentLink(event.getOrderId(), event.getPaymentLink());

        } catch (Exception e) {
            System.err.println("Failed to process message: " + e.getMessage());
        }
    }
}
