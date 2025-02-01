package com.paysys.order.ports.outbound.sync;

public interface SyncServicePort {
    boolean synctoDB(String orderId, String message);
}
