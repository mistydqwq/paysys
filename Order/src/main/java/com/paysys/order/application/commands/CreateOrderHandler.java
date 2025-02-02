package com.paysys.order.application.commands;

import com.paysys.common.BaseResponse;
import com.paysys.common.ErrorCode;
import com.paysys.common.ResultUtils;
import com.paysys.order.domain.entities.Order;
import com.paysys.order.domain.events.OrderCreateEvent;
import com.paysys.order.domain.valueobj.OrderVO;
import com.paysys.order.ports.inbound.CreateOrderUseCase;
import com.paysys.order.ports.outbound.EventPublisherPort;
import com.paysys.order.ports.outbound.OrderRepositoryPort;
import com.paysys.stock.StockServiceApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CreateOrderHandler implements CreateOrderUseCase {

    @Autowired
    private StockServiceApi stockServiceApi;
    @Autowired
    private OrderRepositoryPort orderRepositoryPort;
    @Autowired
    private EventPublisherPort eventPublisherPort;


    @Override
    public BaseResponse<String> createOrder(CreateOrderCommand createOrderCommand) {
        OrderVO orderVO = new OrderVO();
        orderVO.setCustomerId(createOrderCommand.getCustomerId());
        orderVO.setItems(createOrderCommand.getItems());
        orderVO.setNote(createOrderCommand.getNote());
        orderVO.setStatus(0);
        Order order = Order.fromVO(orderVO);
        order.setOrderId(order.generateOrderId());
        order.setTotalAmount(order.calculateTotalAmount());
        order.Pending();

        // check order
        if(!order.isValid()){
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "Params error");
        }

        // reserve stock
        BaseResponse<Boolean>reserveRes= stockServiceApi.reserveStock(order.getOrderId(), order.getItems());
        if(reserveRes.getCode()!=0 || !reserveRes.getData()){
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, reserveRes.getDescription());
        }

        // save order
        order.Created();
        boolean saveRes=orderRepositoryPort.save(order);
        if(!saveRes){
            stockServiceApi.releaseStock(order.getOrderId(), order.getItems());
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "Save order error");
        }

        OrderCreateEvent orderCreatedEvent = new OrderCreateEvent(order.getOrderId(), order.getItems(), order.getNote());
        boolean publishRes=eventPublisherPort.publishEvent(orderCreatedEvent);
        if(!publishRes){
            stockServiceApi.releaseStock(order.getOrderId(), order.getItems());
            orderRepositoryPort.delete(order.getOrderId());
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "Publish event error");
        }

        return ResultUtils.success(order.getOrderId());
    }
}
