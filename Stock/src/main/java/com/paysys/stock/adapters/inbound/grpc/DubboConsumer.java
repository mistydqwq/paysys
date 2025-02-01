package com.paysys.stock.adapters.inbound.grpc;

import com.paysys.common.BaseResponse;
import com.paysys.stock.StockServiceApi;
import com.paysys.stock.ports.inbound.ReleaseStockUseCase;
import com.paysys.stock.ports.inbound.ReserveStockUseCase;
import org.apache.dubbo.config.annotation.DubboService;
import com.paysys.vo.OrderItem;

import java.util.List;

@DubboService(version = "1.0.0")
public class DubboConsumer implements StockServiceApi {
    private final ReserveStockUseCase reserveStockUseCase;
    private final ReleaseStockUseCase releaseStockUseCase;

    public DubboConsumer(ReserveStockUseCase reserveStockUseCase, ReleaseStockUseCase releaseStockUseCase) {
        this.reserveStockUseCase = reserveStockUseCase;
        this.releaseStockUseCase = releaseStockUseCase;
    }

    @Override
    public BaseResponse<Boolean> reserveStock(String orderId, List<OrderItem> list) {
        return reserveStockUseCase.reserveStock(orderId, list);
    }

    @Override
    public boolean releaseStock(String orderId, List<OrderItem> list) {
        return releaseStockUseCase.releaseStock(orderId, list);
    }

}
