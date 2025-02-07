package com.paysys.order.ports.outbound;

public interface RateLimiterPort {
    boolean tryAcquire(String customerId, int permits);
}
