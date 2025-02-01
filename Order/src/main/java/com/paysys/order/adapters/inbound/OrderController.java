package com.paysys.order.adapters.inbound;

import com.paysys.order.application.commands.CreateOrderCommand;
import com.paysys.common.BaseResponse;
import com.paysys.order.ports.inbound.CreateOrderUseCase;
import com.paysys.order.ports.inbound.GetOrderStatusUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private GetOrderStatusUseCase getOrderStatusUseCase;

    @PostMapping("/create")
    public BaseResponse<String> createOrder(@RequestBody CreateOrderCommand createOrderCommand) {
        return createOrderUseCase.createOrder(createOrderCommand);
    }

    @GetMapping("/status/{orderId}")
    public BaseResponse<Integer> getOrderStatus(@PathVariable String orderId) {
        return getOrderStatusUseCase.getOrderStatus(orderId);
    }
}
