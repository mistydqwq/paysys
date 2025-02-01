package com.paysys.stock.ports.outbound;

public interface DistributedLockPort {
    boolean acquireLock(String productId, long timeoutSeconds);
    void releaseLock(String productId);
}