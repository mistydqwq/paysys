package com.paysys.stock.ports.inbound;

import com.paysys.common.BaseResponse;

public interface DeleteStockUseCase {
    BaseResponse<Boolean> deleteStock(String productId);
}
