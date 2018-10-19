package com.eletac.tronwallet;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.ByteString;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.tron.api.GrpcAPI;
import org.tron.protos.Protocol;
import org.tron.walletserver.WalletManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static void saveAccount(Context context, String walletName, Protocol.Account account) {
        if (context != null && account != null && WalletManager.existWallet(walletName)) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(walletName, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putString(context.getString(R.string.name_key), account.getAccountName().toStringUtf8());
            editor.putString(context.getString(R.string.address_key), WalletManager.encode58Check(account.getAddress().toByteArray()));
            editor.putLong(context.getString(R.string.balance_key), account.getBalance());
            editor.putString(context.getString(R.string.assets_key), new Gson().toJson(account.getAssetMap()));

            List<Protocol.Vote> votesList = account.getVotesList();
            Map<String, Long> votesMap = new HashMap<>();
            for (Protocol.Vote vote : votesList) {
                String voteAddress = WalletManager.encode58Check(vote.getVoteAddress().toByteArray());
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

    public static void saveAccountNet(Context context, String walletName, GrpcAPI.AccountNetMessage accountNet) {
        if (context != null && accountNet != null && WalletManager.existWallet(walletName)) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(walletName, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putLong(context.getString(R.string.net_limit_key), accountNet.getNetLimit());
            editor.putLong(context.getString(R.string.net_used_key), accountNet.getNetUsed());
            editor.putLong(context.getString(R.string.net_free_limit_key), accountNet.getFreeNetLimit());
            editor.putLong(context.getString(R.string.net_free_used_key), accountNet.getFreeNetUsed());

            editor.apply();
        }
    }

    public static void saveAccountRes(Context context, String walletName, GrpcAPI.AccountResourceMessage accountRes) {
        if (context != null && accountRes != null && WalletManager.existWallet(walletName)) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(walletName, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putLong(context.getString(R.string.energy_limit_key), accountRes.getEnergyLimit());
            editor.putLong(context.getString(R.string.energy_used_key), accountRes.getEnergyUsed());

            editor.apply();
        }
    }

    public static GrpcAPI.AccountResourceMessage getAccountRes(Context context, String walletName) {
        if(context != null && WalletManager.existWallet(walletName)) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(walletName, Context.MODE_PRIVATE);

            GrpcAPI.AccountResourceMessage.Builder accountResMessage = GrpcAPI.AccountResourceMessage.newBuilder();

            accountResMessage.setEnergyLimit(sharedPreferences.getLong(context.getString(R.string.energy_limit_key), 0));
            accountResMessage.setEnergyUsed(sharedPreferences.getLong(context.getString(R.string.energy_used_key), 0));

            return accountResMessage.build();
        }
        return GrpcAPI.AccountResourceMessage.getDefaultInstance();
    }


    public static GrpcAPI.AccountNetMessage getAccountNet(Context context, String walletName) {
        if(context != null && WalletManager.existWallet(walletName)) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(walletName, Context.MODE_PRIVATE);

            GrpcAPI.AccountNetMessage.Builder accountNetMessage = GrpcAPI.AccountNetMessage.newBuilder();

            accountNetMessage.setNetLimit(sharedPreferences.getLong(context.getString(R.string.net_limit_key), 0));
            accountNetMessage.setNetUsed(sharedPreferences.getLong(context.getString(R.string.net_used_key), 0));
            accountNetMessage.setFreeNetLimit(sharedPreferences.getLong(context.getString(R.string.net_free_limit_key), 0));
            accountNetMessage.setFreeNetUsed(sharedPreferences.getLong(context.getString(R.string.net_free_used_key), 0));

            return accountNetMessage.build();
        }
        return GrpcAPI.AccountNetMessage.getDefaultInstance();
    }

    public static Protocol.Account getAccount(Context context, String walletName) {
        if(context != null && WalletManager.existWallet(walletName)) {
            Protocol.Account.Builder builder = Protocol.Account.newBuilder();

            SharedPreferences sharedPreferences = context.getSharedPreferences(walletName, Context.MODE_PRIVATE);

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
                    byte[] voteAddress = WalletManager.decodeFromBase58Check(entry.getKey());
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
            if(WalletManager.isAddressValid(WalletManager.decodeFromBase58Check(address)))
                builder.setAddress(ByteString.copyFrom(WalletManager.decodeFromBase58Check(address)));
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

    public static double round(double value, int places, RoundingMode mode) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, mode);
        return bd.doubleValue();
    }


    public static String getContractName(Protocol.Transaction.Contract contract) {
        if(contract == null)
            return "";

        switch (contract.getType()) {
            case AccountCreateContract:
                return "AccountCreateContract";
            case TransferContract:
                return "TransferContract";
            case TransferAssetContract:
                return "TransferAssetContract";
            case VoteAssetContract:
                return "VoteAssetContract";
            case VoteWitnessContract:
                return "VoteWitnessContract";
            case WitnessCreateContract:
                return "WitnessCreateContract";
            case AssetIssueContract:
                return "AssetIssueContract";
            case WitnessUpdateContract:
                return "WitnessUpdateContract";
            case ParticipateAssetIssueContract:
                return "ParticipateAssetIssueContract";
            case AccountUpdateContract:
                return "AccountUpdateContract";
            case FreezeBalanceContract:
                return "FreezeBalanceContract";
            case UnfreezeBalanceContract:
                return "UnfreezeBalanceContract";
            case WithdrawBalanceContract:
                return "WithdrawBalanceContract";
            case UnfreezeAssetContract:
                return "UnfreezeAssetContract";
            case UpdateAssetContract:
                return "UpdateAssetContract";
            case CustomContract:
                return "CustomContract";
            case UNRECOGNIZED:
                return "UNRECOGNIZED";
        }
        return "";
    }
}
