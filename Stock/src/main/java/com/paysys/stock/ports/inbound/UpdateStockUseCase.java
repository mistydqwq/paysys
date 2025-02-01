package com.paysys.stock.ports.inbound;

import com.paysys.stock.application.commands.UpdateStockCommand;
import com.paysys.stock.common.BaseResponse;

public interface UpdateStockUseCase {
    BaseResponse<Boolean> updateStock(UpdateStockCommand updateStockCommand);
}