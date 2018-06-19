package org.tron.walletserver;

import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;

public class Wallet {
    private boolean isWatchOnly = false;
    private boolean isColdWallet = false;
    private String walletName;
    private String password;
    private String publicKey;
    private String privateKey;

    public boolean isWatchOnly() {
        return isWatchOnly;
    }

    public void setWatchOnly(boolean watchOnly) {
        isWatchOnly = watchOnly;
    }

    public boolean isColdWallet() {
        return isColdWallet;
    }

    public void setColdWallet(boolean coldWallet) {
        isColdWallet = coldWallet;
    }

    public String getWalletName() {
        return walletName;
    }

    public void setWalletName(String walletName) {
        this.walletName = walletName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String computeAddress() {
        try {
            byte[] pubKeyAsc = publicKey.getBytes();
            byte[] pubKeyHex = Hex.decode(pubKeyAsc);
            return WalletClient.encode58Check(ECKey.fromPublicOnly(pubKeyHex).getAddress());
        } catch (Exception ex) {
            return null;
        }
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
}
