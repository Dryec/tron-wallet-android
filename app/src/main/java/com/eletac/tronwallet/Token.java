package com.eletac.tronwallet;

import org.tron.protos.Contract;

public class Token {
    private String name;
    private long amount;
    private Price price;

    public Token() {

    }

    public Token(String name, long amount) {
        this.name = name;
        this.amount = amount;
    }

    public Price getPrice() {
        return price;
    }

    public void setPrice(Price price) {
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }
}
