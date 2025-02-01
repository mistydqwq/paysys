package com.paysys.order.adapters.outbound.sync;

import com.paysys.order.domain.entities.Order;
import com.paysys.order.ports.outbound.OrderRepositoryPort;

import java.util.Optional;

public class RedisRepositoryImpl implements OrderRepositoryPort {
    @Override
    public Optional<Order> findById(String orderId) {
        return null;
    }

    @Override
    public boolean save(Order order) {
        return false;
    }

    @Override
    public boolean update(Order order) {
        return false;
    }

    @Override
    public boolean updateStatus(String orderId, int status) {
        return false;
    }

    @Override
    public boolean updatePaymentLink(String orderId, String paymentLink) {
        return false;
    }

    @Override
    public boolean delete(String orderId) {
        return false;
    }
}
