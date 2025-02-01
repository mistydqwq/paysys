package com.paysys.stock.ports.inbound;

import com.paysys.stock.common.BaseResponse;

public interface DeleteStockUseCase {
    BaseResponse<Boolean> deleteStock(String productId);
}
