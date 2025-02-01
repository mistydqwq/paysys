package com.paysys.stock.application.queries;

import com.paysys.stock.common.BaseResponse;
import com.paysys.stock.common.ErrorCode;
import com.paysys.stock.common.ResultUtils;
import com.paysys.stock.domain.entities.Stock;
import com.paysys.stock.ports.inbound.GetStockQuantityUseCase;
import com.paysys.stock.ports.outbound.StockRepositoryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GetStockQuantityHandler implements GetStockQuantityUseCase {

    @Autowired
    private StockRepositoryPort stockRepositoryPort;

    @Override
    public BaseResponse<Long> getStockQuantity(String productId) {
        Optional<Stock> optionalStock = stockRepositoryPort.findById(productId);
        if (!optionalStock.isPresent()) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "Stock not found");
        }

        Stock stock = optionalStock.get();
        return ResultUtils.success(stock.getQuantity());
    }

    @Override
    public BaseResponse<Long> getStockAvailableQuantity(String productId) {
        Optional<Stock> optionalStock = stockRepositoryPort.findById(productId);
        if (!optionalStock.isPresent()) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "Stock not found");
        }

        Stock stock = optionalStock.get();
        return ResultUtils.success(stock.getAvailableQuantity());
    }

    @Override
    public BaseResponse<Long> getStockReserveQuantity(String productId) {
        Optional<Stock> optionalStock = stockRepositoryPort.findById(productId);
        if (!optionalStock.isPresent()) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "Stock not found");
        }

        Stock stock = optionalStock.get();
        return ResultUtils.success(stock.getReservedQuantity());
    }
}
