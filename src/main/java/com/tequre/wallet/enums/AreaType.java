package com.tequre.wallet.enums;

public enum AreaType {

    NON_RAPDRP("NON-RAPDRP"),
    RAPDRP("RAPDRP");

    private String name;

    AreaType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
