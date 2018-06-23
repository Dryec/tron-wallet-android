package org.tron.walletserver;

import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;

import java.math.BigInteger;

public class Wallet {
    private ECKey mECKey = null;

    private boolean isWatchOnly = false;
    private boolean isColdWallet = false;
    private String walletName = "";
    private String encPassword = "";
    private String address = "";
    private byte[] encPrivateKey;
    private byte[] publicKey;


    public Wallet() {
    }

    public Wallet(boolean generateECKey) {
        if(generateECKey) {
            mECKey = new ECKey(Utils.getRandom());
        }
    }

    public Wallet(String privateKey) {
        setPrivateKey(privateKey);
    }

    public boolean isOpen() {
        return mECKey != null && mECKey.getPrivKeyBytes() != null;
    }

    public boolean open(String password) {
        if(encPrivateKey != null) {
            setPrivateKey(WalletManager.decryptPrivateKey(encPrivateKey, password));
            return isOpen();
        } else {
            return false;
        }
    }

    public byte[] getPublicKey() {
        return mECKey != null ? mECKey.getPubKey() : publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public byte[] getPrivateKey() {
        return mECKey != null ? mECKey.getPrivKeyBytes() : null;
    }

    public void setPrivateKey(String privateKey) {
        if(privateKey != null && !privateKey.isEmpty()) {
            ECKey tempKey = null;
            try {
                BigInteger priK = new BigInteger(privateKey, 16);
                tempKey = ECKey.fromPrivate(priK);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            mECKey = tempKey;
        } else {
            mECKey = null;
        }
    }


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

    public String getEncryptedPassword() {
        return encPassword;
    }

    public void setEncryptedPassword(String password) {
        this.encPassword = password;
    }

    public String getAddress() {
        if(mECKey != null) {
            return WalletManager.encode58Check(mECKey.getAddress());
        }
        else if (publicKey != null){
            return WalletManager.encode58Check(ECKey.fromPublicOnly(publicKey).getAddress());
        } else {
            return address;
        }
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public ECKey getECKey() {
        return mECKey;
    }

    public byte[] getEncryptedPrivateKey() {
        return encPrivateKey;
    }

    public void setEncryptedPrivateKey(byte[] encPrivateKey) {
        this.encPrivateKey = encPrivateKey;
    }
}
