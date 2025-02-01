package com.paysys.order.adapters.outbound;


import com.paysys.stock.StockServiceApi;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import com.paysys.common.BaseResponse;
import com.paysys.vo.OrderItem;

import java.util.List;

@Service
public class StockServiceAdapter implements StockServiceApi {

    @DubboReference(version = "1.0.0", check = false)
    private StockServiceApi stockServiceApi;

    @Override
    public BaseResponse<Boolean> reserveStock(String orderId, List<OrderItem> list) {
        return stockServiceApi.reserveStock(orderId, list);
        //return null;
    }

    @Override
    public boolean releaseStock(String orderId, List<OrderItem> list) {
        return stockServiceApi.releaseStock(orderId, list);
        //return false;
    }
}
