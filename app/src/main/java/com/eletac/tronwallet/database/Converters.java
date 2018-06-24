package com.eletac.tronwallet.database;

import android.arch.persistence.room.TypeConverter;

import com.google.protobuf.InvalidProtocolBufferException;

import org.spongycastle.util.encoders.Hex;
import org.tron.protos.Protocol;

public class Converters {
    @TypeConverter
    public static String transactionToHexString(Protocol.Transaction transaction) {
        return Hex.toHexString(transaction.toByteArray());
    }

    @TypeConverter
    public static Protocol.Transaction hexStringToTransaction(String hexString) {
        try {
            return Protocol.Transaction.parseFrom(Hex.decode(hexString));
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return null;
        }
    }
}
