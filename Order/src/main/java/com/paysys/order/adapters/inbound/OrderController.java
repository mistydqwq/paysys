package com.paysys.order.adapters.inbound;

import com.paysys.common.ErrorCode;
import com.paysys.common.ResultUtils;
import com.paysys.order.application.commands.CreateOrderCommand;
import com.paysys.common.BaseResponse;
import com.paysys.order.ports.inbound.CreateOrderUseCase;
import com.paysys.order.ports.inbound.GetOrderStatusUseCase;
import com.paysys.order.ports.outbound.RateLimiterPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private GetOrderStatusUseCase getOrderStatusUseCase;

    @Autowired
    private RateLimiterPort rateLimiterPort;

    @PostMapping("/create")
    public BaseResponse<String> createOrder(@RequestBody CreateOrderCommand createOrderCommand) {
        return createOrderUseCase.createOrder(createOrderCommand);
    }

    @GetMapping("/status/{orderId}")
    public BaseResponse<Integer> getOrderStatus(@PathVariable String orderId) {
        return getOrderStatusUseCase.getOrderStatus(orderId);
    }

    @PostMapping("/create/seckill")
    public BaseResponse<String> createSeckillOrder(@RequestBody CreateOrderCommand createOrderCommand) {
        if(!rateLimiterPort.tryAcquire(createOrderCommand.getCustomerId(), 1)){
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "Request is limited");
        }
        return createOrderUseCase.createOrder(createOrderCommand);
    }
}
