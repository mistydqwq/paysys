package com.paysys.stock.application.commands;

import com.paysys.common.BaseResponse;
import com.paysys.common.ResultUtils;
import com.paysys.stock.ports.inbound.ReserveStockUseCase;
import com.paysys.stock.ports.outbound.StockRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.paysys.vo.OrderItem;

import java.util.List;

@Slf4j
@Service
public class ReserveStockHandler implements ReserveStockUseCase {

    @Autowired
    private StockRepositoryPort stockRepositoryPort;

    @Override
    @Transactional
    public BaseResponse<Boolean> reserveStock(String orderId, List<OrderItem> list) {
//        if (orderId == null || orderId.isEmpty()) {
//            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "Invalid order id");
//        }
//        if (list == null || list.isEmpty()) {
//            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "Invalid order items");
//        }
//
//        try {
//            boolean success = stockRepositoryPort.reserveStock(orderId, list);
//            return success ? ResultUtils.success(true) : ResultUtils.error(ErrorCode.SYSTEM_ERROR, "Failed to reserve stock");
//        } catch (Exception e) {
//            log.error("Failed to reserve stock for order: {}, error: {}", orderId, e.getMessage());
//            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "System error when reserving stock");
//        }
        return ResultUtils.success(true);
    }
}
