package com.eletac.tronwallet;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.arasthel.asyncjob.AsyncJob;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.tron.api.GrpcAPI;
import org.tron.protos.Protocol;
import org.tron.walletserver.GrpcClient;
import org.tron.walletserver.WalletClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.grpc.StatusRuntimeException;

public class Utils {
    public static Bitmap strToQR(String str, int width, int height) {
        if(str == null || str.equals("")) {
            return null;
        }
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(str, BarcodeFormat.QR_CODE,width,height);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            return barcodeEncoder.createBitmap(bitMatrix);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int pxToDp(Context context, int px) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int dp = Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return dp;
    }

    public static int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    public static byte[] transactionToByteArray(Protocol.Transaction transaction) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        transaction.writeTo(outputStream);
        outputStream.flush();
        return outputStream.toByteArray();
    }

    public static Protocol.Transaction parseTransaction(byte[] transactionBytes) throws InvalidProtocolBufferException {
        return Protocol.Transaction.parseFrom(transactionBytes);
    }

    public static void saveAccount(Context context, Protocol.Account account) {
        if (context != null && account != null) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_account_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putString(context.getString(R.string.name_key), account.getAccountName().toStringUtf8());
            editor.putString(context.getString(R.string.address_key), WalletClient.encode58Check(account.getAddress().toByteArray()));
            editor.putLong(context.getString(R.string.balance_key), account.getBalance());
            editor.putString(context.getString(R.string.assets_key), new Gson().toJson(account.getAssetMap()));

            List<Protocol.Vote> votesList = account.getVotesList();
            Map<String, Long> votesMap = new HashMap<>();
            for (Protocol.Vote vote : votesList) {
                String voteAddress = WalletClient.encode58Check(vote.getVoteAddress().toByteArray());
                if(!voteAddress.equals(""))
                    votesMap.put(voteAddress, vote.getVoteCount());
            }
            editor.putString(context.getString(R.string.votes_key), new Gson().toJson(votesMap));

            List<Protocol.Account.Frozen> frozenList = account.getFrozenList();
            Map<Long, Long> frozenMap = new HashMap<>();
            for (Protocol.Account.Frozen frozen : frozenList) {
                long balance = frozen.getFrozenBalance();
                if(frozenMap.containsKey(frozen.getExpireTime())) {
                    balance += frozenMap.get(frozen.getExpireTime());
                }
                frozenMap.put(frozen.getExpireTime(), balance);
            }
            editor.putString(context.getString(R.string.frozen_key), new Gson().toJson(frozenMap));

            editor.putLong(context.getString(R.string.bandwidth_key), account.getNetUsage());
            editor.putLong(context.getString(R.string.create_time_key), account.getCreateTime());
            editor.putLong(context.getString(R.string.latest_operation_time_key), account.getLatestOprationTime());
            editor.apply();
        }
    }

    public static void saveAccountNet(Context context, GrpcAPI.AccountNetMessage accountNet) {
        if (context != null && accountNet != null) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_account_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putLong(context.getString(R.string.net_limit_key), accountNet.getNetLimit());
            editor.putLong(context.getString(R.string.net_used_key), accountNet.getNetUsed());
            editor.putLong(context.getString(R.string.net_free_limit_key), accountNet.getFreeNetLimit());
            editor.putLong(context.getString(R.string.net_free_used_key), accountNet.getFreeNetUsed());
            // TODO asset net usage

            editor.apply();
        }
    }

    public static GrpcAPI.AccountNetMessage getAccountNet(Context context) {
        if(context != null) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_account_file_key), Context.MODE_PRIVATE);

            GrpcAPI.AccountNetMessage.Builder accountNetMessage = GrpcAPI.AccountNetMessage.newBuilder();

            accountNetMessage.setNetLimit(sharedPreferences.getLong(context.getString(R.string.net_limit_key), 0));
            accountNetMessage.setNetUsed(sharedPreferences.getLong(context.getString(R.string.net_used_key), 0));
            accountNetMessage.setFreeNetLimit(sharedPreferences.getLong(context.getString(R.string.net_free_limit_key), 0));
            accountNetMessage.setFreeNetUsed(sharedPreferences.getLong(context.getString(R.string.net_free_used_key), 0));

            return accountNetMessage.build();
        }
        return GrpcAPI.AccountNetMessage.getDefaultInstance();
    }

    public static Protocol.Account getAccount(Context context) {
        if(context != null) {
            Protocol.Account.Builder builder = Protocol.Account.newBuilder();

            SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_account_file_key), Context.MODE_PRIVATE);

            String name = sharedPreferences.getString(context.getString(R.string.name_key),"");
            String address = sharedPreferences.getString(context.getString(R.string.address_key),"");
            long balance = sharedPreferences.getLong(context.getString(R.string.balance_key),0);
            Map<String, Long> assets =
                    new Gson()
                            .fromJson(
                                    sharedPreferences.getString(context.getString(R.string.assets_key), ""),
                                    new TypeToken<Map<String, Long>>(){}.getType());

            List<Protocol.Vote> votes = new ArrayList<>();
            Map<String, Long> votesMap =
                    new Gson()
                            .fromJson(
                                    sharedPreferences.getString(context.getString(R.string.votes_key), ""),
                                    new TypeToken<Map<String, Long>>() {
                                    }.getType());
            if (votesMap != null) {
                for (Map.Entry<String, Long> entry : votesMap.entrySet()) {
                    byte[] voteAddress = WalletClient.decodeFromBase58Check(entry.getKey());
                    if(voteAddress != null) {
                        Protocol.Vote.Builder voteBuilder = Protocol.Vote.newBuilder();
                        voteBuilder.setVoteAddress(ByteString.copyFrom(voteAddress));
                        voteBuilder.setVoteCount(entry.getValue());
                        votes.add(voteBuilder.build());
                    }
                }
            }

            List<Protocol.Account.Frozen> frozen = new ArrayList<>();
            Map<Long, Long> frozenMap =
                    new Gson()
                            .fromJson(
                                    sharedPreferences.getString(context.getString(R.string.frozen_key), ""),
                                    new TypeToken<Map<Long, Long>>() {
                                    }.getType());
            if (frozenMap != null) {
                for (Map.Entry<Long, Long> entry : frozenMap.entrySet()) {
                    Protocol.Account.Frozen.Builder frozenBuilder = Protocol.Account.Frozen.newBuilder();
                    frozenBuilder.setExpireTime(entry.getKey());
                    frozenBuilder.setFrozenBalance(entry.getValue());
                    frozen.add(frozenBuilder.build());
                }
            }



            long bandwidth = sharedPreferences.getLong(context.getString(R.string.bandwidth_key),0);
            long createTime = sharedPreferences.getLong(context.getString(R.string.create_time_key),0);
            long latestOperationTime = sharedPreferences.getLong(context.getString(R.string.latest_operation_time_key),0);


            builder.setAccountName(ByteString.copyFromUtf8(name));
            if(WalletClient.addressValid(WalletClient.decodeFromBase58Check(address)))
                builder.setAddress(ByteString.copyFrom(WalletClient.decodeFromBase58Check(address)));
            builder.setBalance(balance);
            if(assets != null)
                builder.putAllAsset(assets);
            builder.addAllVotes(votes);
            builder.addAllFrozen(frozen);
            builder.setNetUsage(bandwidth);
            builder.setCreateTime(createTime);
            builder.setLatestOprationTime(latestOperationTime);
            return builder.build();
        }
        return Protocol.Account.getDefaultInstance();
    }

    public static long getAccountAssetAmount(Protocol.Account account, String assetName) {
        Map<String, Long> assets = account.getAssetMap();
        return assets.containsKey(assetName) ? assets.get(assetName) : 0;
    }

    public static String getPublicAddress(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        return sharedPreferences.getString(context.getString(R.string.public_address_raw), "");
    }
}
