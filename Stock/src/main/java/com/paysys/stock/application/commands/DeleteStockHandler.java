package com.paysys.stock.application.commands;

import com.paysys.stock.common.BaseResponse;
import com.paysys.stock.common.ErrorCode;
import com.paysys.stock.common.ResultUtils;
import com.paysys.stock.ports.inbound.DeleteStockUseCase;
import com.paysys.stock.ports.outbound.StockRepositoryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteStockHandler implements DeleteStockUseCase {

    @Autowired
    private StockRepositoryPort stockRepositoryPort;

    @Override
    @Transactional
    public BaseResponse<Boolean> deleteStock(String productId) {
        // Validate parameters
        if (productId == null || productId.isEmpty()) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "Invalid product id");
        }

        // Delete stock
        try {
            boolean delete = stockRepositoryPort.delete(productId);
            if (!delete) {
                return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "Failed to delete stock");
            }

            return ResultUtils.success(true);
        } catch (Exception e) {
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "System error when deleting stock");
        }
    }
}
