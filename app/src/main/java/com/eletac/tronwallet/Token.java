package com.eletac.tronwallet;

import org.tron.protos.Contract;

public class Token {
    private Contract.AssetIssueContract assetIssueContract;
    private Price price;

    public Token() {

    }

    public Token(Contract.AssetIssueContract contract) {
        assetIssueContract = contract;
    }

    public Contract.AssetIssueContract getAssetIssueContract() {
        return assetIssueContract;
    }

    public void setAssetIssueContract(Contract.AssetIssueContract assetIssueContract) {
        this.assetIssueContract = assetIssueContract;
    }

    public Price getPrice() {
        return price;
    }

    public void setPrice(Price price) {
        this.price = price;
    }
}
