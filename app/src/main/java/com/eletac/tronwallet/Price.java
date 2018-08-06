package com.eletac.tronwallet;

public class Price {
    private float price = 0.f;
    private float change_1h = 0.f;
    private float change_24h = 0.f;
    private float change_7d = 0.f;

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public float getChange_1h() {
        return change_1h;
    }

    public void setChange_1h(float change_1h) {
        this.change_1h = change_1h;
    }

    public float getChange_24h() {
        return change_24h;
    }

    public void setChange_24h(float change_24h) {
        this.change_24h = change_24h;
    }

    public float getChange_7d() {
        return change_7d;
    }

    public void setChange_7d(float change_7d) {
        this.change_7d = change_7d;
    }
}