package com.paysys.stock.ports.inbound;

import com.paysys.stock.common.BaseResponse;

public interface GetStockQuantityUseCase {
    BaseResponse<Long> getStockQuantity(String productId);
    BaseResponse<Long> getStockAvailableQuantity(String productId);
    BaseResponse<Long> getStockReserveQuantity(String productId);
}
