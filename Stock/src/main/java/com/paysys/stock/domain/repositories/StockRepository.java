package com.paysys.stock.domain.repositories;

import com.paysys.stock.domain.entities.Stock;

import java.util.Optional;

public interface StockRepository {
    Optional<Stock> findById(String productId);
    boolean save(Stock stock);
    boolean update(Stock stock);
    boolean delete(String productId);
}
