package com.tequre.wallet.enums;

public enum PaymentType {

    NON_RAPDRP("NON-RAPDRP"),
    RAPDRP("RAPDRP"),
    BANK("BANK"),
    BILL("BILL"),
    COMMISSION("COMMISSION"),
    REFUND("REFUND"),
    WALLET("WALLET"),
    CANCELLED("CANCELLED");

    private String name;

    PaymentType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
