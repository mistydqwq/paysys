package com.paysys.stock.ports.outbound.sync;

public interface SyncServicePort {
    boolean synctoDB(String productId, String message);
}
