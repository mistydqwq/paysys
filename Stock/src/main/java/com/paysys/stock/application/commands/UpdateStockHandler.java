package com.paysys.stock.application.commands;

import com.paysys.common.BaseResponse;
import com.paysys.common.ErrorCode;
import com.paysys.common.ResultUtils;
import com.paysys.stock.domain.entities.Stock;
import com.paysys.stock.ports.inbound.UpdateStockUseCase;
import com.paysys.stock.ports.outbound.StockRepositoryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UpdateStockHandler implements UpdateStockUseCase {

    @Qualifier("redisRepositoryImpl")
    @Autowired
    private StockRepositoryPort stockRepositoryPort;

    @Override
    @Transactional
    public BaseResponse<Boolean> updateStock(UpdateStockCommand updateStockCommand) {
        // Validate parameters
        if (updateStockCommand == null || updateStockCommand.getProductId() == null || updateStockCommand.getQuantity() == null || updateStockCommand.getQuantity() < 0) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "Invalid update stock parameters");
        }

        // Find stock by product id
        Optional<Stock> optionalStock = stockRepositoryPort.findById(updateStockCommand.getProductId());

        // If stock not found, create new stock
        Stock stock;
        if (!optionalStock.isPresent()) {
            stock = new Stock();
            stock.setProductId(updateStockCommand.getProductId());
            stock.setQuantity(updateStockCommand.getQuantity());
            stock.setReservedQuantity(0L);
            stock.setProductName("Product " + updateStockCommand.getProductId());
            if (updateStockCommand.getPrice() != null) {
                stock.setPrice(updateStockCommand.getPrice());
            }

            try {
                boolean save = stockRepositoryPort.save(stock);
                if (!save) {
                    return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "Failed to create new stock");
                }
                return ResultUtils.success(true);
            } catch (Exception e) {
                return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "System error when creating new stock");
            }
        }

        // Update stock
        stock = optionalStock.get();
        try {
            stock.setQuantity(updateStockCommand.getQuantity());
            if (updateStockCommand.getPrice() != null) {
                stock.setPrice(updateStockCommand.getPrice());
            }

            if (stock.getReservedQuantity() > stock.getQuantity()) {
                return ResultUtils.error(ErrorCode.PARAMS_ERROR, "Reserved quantity cannot be greater than total quantity");
            }

            boolean update = stockRepositoryPort.update(stock);
            if (!update) {
                return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "Failed to update stock");
            }
            return ResultUtils.success(true);
        } catch (Exception e) {
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "System error when updating stock");
        }
    }
}
