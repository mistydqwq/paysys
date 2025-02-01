package com.paysys.stock.adapters.inbound.web;

import com.paysys.stock.application.commands.UpdateStockCommand;
import com.paysys.common.BaseResponse;
import com.paysys.stock.ports.inbound.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    private final GetStockQuantityUseCase getStockQuantityUseCase;
    private final UpdateStockUseCase updateStockUseCase;
    private final DeleteStockUseCase deleteStockUseCase;

    @GetMapping("/{productId}")
    public BaseResponse<Long> getStockQuantity(@PathVariable String productId) {
        log.info("getStockQuantity: productId={}", productId);
        return getStockQuantityUseCase.getStockQuantity(productId);
    }

    @GetMapping("/{productId}/reserve")
    public BaseResponse<Long> getStockReserveQuantity(@PathVariable String productId) {
        log.info("getStockReserveQuantity: productId={}", productId);
        return getStockQuantityUseCase.getStockReserveQuantity(productId);
    }

    @GetMapping("/{productId}/available")
    public BaseResponse<Long> getAvailableStockQuantity(@PathVariable String productId) {
        log.info("getStockAvailableQuantity: productId={}", productId);
        return getStockQuantityUseCase.getStockAvailableQuantity(productId);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateStock(@RequestBody UpdateStockCommand updateStockCommand) {
        log.info("updateStock: productId={}, quantity={}, price={}", updateStockCommand.getProductId(), updateStockCommand.getQuantity(), updateStockCommand.getPrice());
        return updateStockUseCase.updateStock(updateStockCommand);
    }

    @DeleteMapping("/{productId}")
    public BaseResponse<Boolean> deleteStock(@PathVariable String productId) {
        log.info("deleteStock: productId={}", productId);
        return deleteStockUseCase.deleteStock(productId);
    }
}