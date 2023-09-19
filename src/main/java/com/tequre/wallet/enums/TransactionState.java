package com.tequre.wallet.enums;

public enum TransactionState {

    // For successful transactions
    SUCCESS,
    // For reverted transactions
    REVERTED,
    // For successful transactions that are rollback
    ROLLBACK,
    // For successful transactions that are cancelled
    CANCELLED;
}
