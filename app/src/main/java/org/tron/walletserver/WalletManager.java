package org.tron.walletserver;

import android.content.Context;
import android.content.SharedPreferences;

import com.eletac.tronwallet.R;
import com.eletac.tronwallet.TronWalletApplication;
import com.google.protobuf.ByteString;

import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountNetMessage;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.BlockList;
import org.tron.api.GrpcAPI.NodeList;
import org.tron.api.GrpcAPI.TransactionList;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.common.crypto.Hash;
import org.tron.common.crypto.SymmEncoder;
import org.tron.common.utils.Base58;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.TransactionUtils;
import org.tron.core.config.Parameter.CommonConstant;
import org.tron.protos.Contract;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Contract.FreezeBalanceContract;
import org.tron.protos.Contract.UnfreezeBalanceContract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Witness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

class AccountComparator implements Comparator {

    public int compare(Object o1, Object o2) {
        return Long.compare(((Account) o2).getBalance(), ((Account) o1).getBalance());
    }
}

class WitnessComparator implements Comparator {

    public int compare(Object o1, Object o2) {
        return Long.compare(((Witness) o2).getVoteCount(), ((Witness) o1).getVoteCount());
    }
}

public class WalletManager {

    private static GrpcClient rpcCli;

    public static void initGRPC() {
        if (rpcCli != null) {
            rpcCli.shutdown();
        }

        Context context = TronWalletApplication.getAppContext();
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        String ip = "", ip_sol = "";
        int port = 0, port_sol = 0;

        try {
            ip = sharedPreferences.getString(context.getString(R.string.ip_key), context.getString(R.string.fullnode_ip));
            port = sharedPreferences.getInt(context.getString(R.string.port_key), Integer.parseInt(context.getString(R.string.fullnode_port)));

            ip_sol = sharedPreferences.getString(context.getString(R.string.ip_sol_key), context.getString(R.string.soliditynode_ip));
            port_sol = sharedPreferences.getInt(context.getString(R.string.port_sol_key), Integer.parseInt(context.getString(R.string.soliditynode_port)));

        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        rpcCli = new GrpcClient(
                !ip.equals("") ? String.format(Locale.US, "%s:%d", ip, port) : "",
                !ip_sol.equals("") ? String.format(Locale.US, "%s:%d", ip_sol, port_sol) : "");
    }

    public static void store(Wallet wallet, String password) throws DuplicateNameException, InvalidPasswordException, InvalidNameException {
        if (!wallet.isWatchOnly() && (wallet.getECKey() == null || wallet.getECKey().getPrivKey() == null)) {
            throw new NullPointerException("Private Key is null");
        }

        if(!wallet.isWatchOnly() && !isPasswordValid(password)) {
            throw new InvalidPasswordException("");
        }
        if(!isNameValid(wallet.getWalletName())) {
            throw new InvalidNameException("");
        }

        Context context = TronWalletApplication.getAppContext();
        if (context == null) {
            throw new NullPointerException("Context is null");
        }

        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedEditor = sharedPreferences.edit();

        Set<String> walletNames = new HashSet<>(sharedPreferences.getStringSet(context.getString(R.string.wallets_key), new HashSet<>()));

        if (walletNames.contains(wallet.getWalletName())) {
            throw new DuplicateNameException("Wallet name already exist");
        }
        walletNames.add(wallet.getWalletName());

        sharedEditor.putStringSet(context.getString(R.string.wallets_key), walletNames);
        sharedEditor.commit();


        SharedPreferences walletPreferences = context.getSharedPreferences(wallet.getWalletName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor walletEditor = walletPreferences.edit();

        // SAVE IS WATCH ONLY
        walletEditor.putBoolean(context.getString(R.string.is_watch_only_setup_key), wallet.isWatchOnly());
        // SAVE IS COLD WALLET
        walletEditor.putBoolean(context.getString(R.string.is_cold_wallet_key), wallet.isColdWallet() && !wallet.isWatchOnly());
        // SAVE NAME
        walletEditor.putString(context.getString(R.string.wallet_name_key), wallet.getWalletName());
        // SAVE PUBKEY
        walletEditor.putString(context.getString(R.string.wallet_address_key), wallet.getAddress());

        if(!wallet.isWatchOnly()) {

            byte[] pwd = getPasswordHash(password);
            String pwdAsc = ByteArray.toHexString(pwd);

            //encrypted by password
            byte[] aseKey = getEncKey(password);

            byte[] privKeyPlain = wallet.getECKey().getPrivKeyBytes();
            byte[] privKeyEnced = SymmEncoder.AES128EcbEnc(privKeyPlain, aseKey);
            String privKeyStr = ByteArray.toHexString(privKeyEnced);

            byte[] pubKeyBytes = wallet.getECKey().getPubKey();
            String pubKeyStr = ByteArray.toHexString(pubKeyBytes);

            // SAVE PASSWORD
            walletEditor.putString(context.getString(R.string.pwd_key), pwdAsc);
            // SAVE PUBKEY
            walletEditor.putString(context.getString(R.string.pub_key), pubKeyStr);
            // SAVE PRIKEY
            walletEditor.putString(context.getString(R.string.priv_key), privKeyStr);
        }

        walletEditor.commit();
    }

    public static void storeWatchOnly(Wallet wallet) throws InvalidNameException, DuplicateNameException {
        wallet.setWatchOnly(true);
        try {
            store(wallet, null);
        } catch (InvalidPasswordException e) {
            e.printStackTrace();
        }
    }

    public static boolean existWallet(String walletName) {
        Context context = TronWalletApplication.getAppContext();
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        Set<String> walletNames = sharedPreferences.getStringSet(context.getString(R.string.wallets_key), null);

        return walletNames != null && walletNames.contains(walletName);
    }

    public static boolean existAnyWallet() {
        return !getWalletNames().isEmpty();
    }

    public static Set<String> getWalletNames() {
        Context context = TronWalletApplication.getAppContext();
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        return new HashSet<>(sharedPreferences.getStringSet(context.getString(R.string.wallets_key), new HashSet<>()));
    }

    public static Wallet getSelectedWallet() {
        Context context = TronWalletApplication.getAppContext();
        SharedPreferences pref = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        return getWallet(pref.getString(context.getString(R.string.selected_wallet_key), ""));
    }

    public static Wallet getWallet(String walletName, String password) {
        if (existWallet(walletName)) {
            Context context = TronWalletApplication.getAppContext();
            SharedPreferences walletPref = context.getSharedPreferences(walletName, Context.MODE_PRIVATE);

            String privateKey = null;
            byte[] priKeyHexEnced = null;


            String privateKeyEncrypted = walletPref.getString(context.getString(R.string.priv_key), "");
            if (!privateKeyEncrypted.isEmpty()) {
                byte[] priKeyAscEnced = privateKeyEncrypted.getBytes();
                priKeyHexEnced = Hex.decode(priKeyAscEnced);

                if(password != null && checkPassword(walletName, password)) {
                    byte[] aesKey = getEncKey(password);
                    byte[] priKeyHexPlain = SymmEncoder.AES128EcbDec(priKeyHexEnced, aesKey);
                    if (priKeyHexPlain != null) {
                        privateKey = Hex.toHexString(priKeyHexPlain);
                    }
                }
            }

            Wallet wallet = new Wallet(privateKey);

            wallet.setWatchOnly(walletPref.getBoolean(context.getString(R.string.is_watch_only_setup_key), false));
            wallet.setColdWallet(walletPref.getBoolean(context.getString(R.string.is_cold_wallet_key), false));
            wallet.setWalletName(walletPref.getString(context.getString(R.string.wallet_name_key), ""));
            wallet.setEncryptedPassword(walletPref.getString(context.getString(R.string.pwd_key), ""));
            wallet.setAddress(walletPref.getString(context.getString(R.string.wallet_address_key), ""));
            wallet.setEncryptedPrivateKey(priKeyHexEnced);

            String publicKeyStr = walletPref.getString(context.getString(R.string.pub_key), "");
            if(!publicKeyStr.isEmpty()) {
                byte[] publicKey = Hex.decode(publicKeyStr.getBytes());
                wallet.setPublicKey(publicKey);
            }

            return wallet;
        }

        return null;
    }

    public static Wallet getWallet(String walletName) {
        return getWallet(walletName, null);
    }

    public static void selectWallet(String walletName) {
        Context context = TronWalletApplication.getAppContext();
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (existWallet(walletName)) {
            editor.putString(context.getString(R.string.selected_wallet_key), walletName);
            editor.commit();
        }
    }

    public static String decryptPrivateKey(byte[] privateKey, String password) {
        byte[] aesKey = getEncKey(password);
        byte[] priKeyHexPlain = SymmEncoder.AES128EcbDec(privateKey, aesKey);

        if (priKeyHexPlain != null) {
            return Hex.toHexString(priKeyHexPlain);
        }
        else
        {
            return "";
        }
    }

    public static Account queryAccount(byte[] address, boolean useSolidity) {
        return rpcCli.queryAccount(address, useSolidity);
    }

    public static Transaction createTransferAssetTransaction(byte[] to, byte[] assertName,
                                                             byte[] owner, long amount) {
        Contract.TransferAssetContract contract = createTransferAssetContract(to, assertName, owner,
                amount);
        return rpcCli.createTransferAssetTransaction(contract);
    }

    public static Transaction createParticipateAssetIssueTransaction(byte[] to, byte[] assertName,
                                                                     byte[] owner, long amount) {
        Contract.ParticipateAssetIssueContract contract = createParticipateAssetIssueContract(to, assertName,
                owner, amount);
        return rpcCli.createParticipateAssetIssueTransaction(contract);
    }

    public static Transaction createUpdateAccountTransaction(byte[] addressBytes, String accountName) {
        Contract.AccountUpdateContract contract = createAccountUpdateContract(ByteArray.fromString(accountName),
                addressBytes);
        return rpcCli.createTransaction(contract);
    }

    public static boolean broadcastTransaction(Transaction transaction) {
        return TransactionUtils.validTransaction(transaction)
                && rpcCli.broadcastTransaction(transaction);
    }

    public static Transaction createWitnessTransaction(byte[] owner, byte[] url) {
        Contract.WitnessCreateContract contract = createWitnessCreateContract(owner, url);
        return rpcCli.createWitness(contract);
    }

    public static Transaction createUpdateWitnessTransaction(byte[] owner, byte[] url) {
        Contract.WitnessUpdateContract contract = createWitnessUpdateContract(owner, url);
        return rpcCli.updateWitness(contract);
    }

    public static Transaction createVoteWitnessTransaction(byte[] owner,
                                                           HashMap<String, String> witness) {
        Contract.VoteWitnessContract contract = createVoteWitnessContract(owner, witness);
        return rpcCli.voteWitnessAccount(contract);
    }

    public static Contract.AssetIssueContract createAssetIssueContract(byte[] owner, String name, String abbr, long totalSupply, int trxNum, int icoNum,
                                                                       long startTime, long endTime, int voteScore, String description, String url,
                                                                       long freeNetLimit, long publicFreeNetLimit, List<AssetIssueContract.FrozenSupply> frozenSupply) {
        Contract.AssetIssueContract.Builder builder = Contract.AssetIssueContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));

        builder.setName(ByteString.copyFrom(name.getBytes()));
        builder.setAbbr(ByteString.copyFrom(abbr.getBytes()));

        if (totalSupply <= 0) {
            return null;
        }
        builder.setTotalSupply(totalSupply);
        if (trxNum <= 0) {
            return null;
        }
        builder.setTrxNum(trxNum);
        if (icoNum <= 0) {
            return null;
        }
        builder.setNum(icoNum);
        long now = System.currentTimeMillis();
        if (startTime <= now) {
            return null;
        }
        if (endTime <= startTime) {
            return null;
        }
        if (freeNetLimit < 0) {
            return null;
        }
        if (publicFreeNetLimit < 0) {
            return null;
        }

        builder.setStartTime(startTime);
        builder.setEndTime(endTime);
        builder.setVoteScore(voteScore);
        builder.setDescription(ByteString.copyFrom(description.getBytes()));
        builder.setUrl(ByteString.copyFrom(url.getBytes()));
        builder.setFreeAssetNetLimit(freeNetLimit);
        builder.setPublicFreeAssetNetLimit(publicFreeNetLimit);

        builder.addAllFrozenSupply(frozenSupply);

        return builder.build();
    }

    public static Transaction createAssetIssueTransaction(Contract.AssetIssueContract contract) {
        return rpcCli.createAssetIssue(contract);
    }

    public static Block getBlock(long blockNum, boolean useSolidity) {
        return rpcCli.getBlock(blockNum, useSolidity);
    }

    public static Contract.TransferContract createTransferContract(byte[] to, byte[] owner,
                                                                   long amount) {
        Contract.TransferContract.Builder builder = Contract.TransferContract.newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsOwner = ByteString.copyFrom(owner);
        builder.setToAddress(bsTo);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        return builder.build();
    }

    public static Contract.TransferAssetContract createTransferAssetContract(byte[] to,
                                                                             byte[] assertName, byte[] owner,
                                                                             long amount) {
        Contract.TransferAssetContract.Builder builder = Contract.TransferAssetContract.newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsName = ByteString.copyFrom(assertName);
        ByteString bsOwner = ByteString.copyFrom(owner);
        builder.setToAddress(bsTo);
        builder.setAssetName(bsName);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        return builder.build();
    }

    public static Contract.ParticipateAssetIssueContract createParticipateAssetIssueContract(byte[] to,
                                                                                             byte[] assertName, byte[] owner,
                                                                                             long amount) {
        Contract.ParticipateAssetIssueContract.Builder builder = Contract.ParticipateAssetIssueContract
                .newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsName = ByteString.copyFrom(assertName);
        ByteString bsOwner = ByteString.copyFrom(owner);
        builder.setToAddress(bsTo);
        builder.setAssetName(bsName);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        return builder.build();
    }

    public static Transaction createTransaction4Transfer(Contract.TransferContract contract) {
        Transaction transaction = rpcCli.createTransaction(contract);
        return transaction;
    }

    public static Contract.AccountUpdateContract createAccountUpdateContract(byte[] accountName,
                                                                             byte[] address) {
        Contract.AccountUpdateContract.Builder builder = Contract.AccountUpdateContract.newBuilder();
        ByteString basAddreess = ByteString.copyFrom(address);
        ByteString bsAccountName = ByteString.copyFrom(accountName);
        builder.setAccountName(bsAccountName);
        builder.setOwnerAddress(basAddreess);

        return builder.build();
    }

    public static Contract.UpdateAssetContract createUpdateAssetContract(
            byte[] address,
            byte[] description,
            byte[] url,
            long newLimit,
            long newPublicLimit
    ) {
        Contract.UpdateAssetContract.Builder builder =
                Contract.UpdateAssetContract.newBuilder();
        ByteString basAddreess = ByteString.copyFrom(address);
        builder.setDescription(ByteString.copyFrom(description));
        builder.setUrl(ByteString.copyFrom(url));
        builder.setNewLimit(newLimit);
        builder.setNewPublicLimit(newPublicLimit);
        builder.setOwnerAddress(basAddreess);

        return builder.build();
    }

    public static Contract.WitnessCreateContract createWitnessCreateContract(byte[] owner,
                                                                             byte[] url) {
        Contract.WitnessCreateContract.Builder builder = Contract.WitnessCreateContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setUrl(ByteString.copyFrom(url));

        return builder.build();
    }

    public static Contract.WitnessUpdateContract createWitnessUpdateContract(byte[] owner,
                                                                             byte[] url) {
        Contract.WitnessUpdateContract.Builder builder = Contract.WitnessUpdateContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setUpdateUrl(ByteString.copyFrom(url));

        return builder.build();
    }

    public static Contract.VoteWitnessContract createVoteWitnessContract(byte[] owner,
                                                                         HashMap<String, String> witness) {
        Contract.VoteWitnessContract.Builder builder = Contract.VoteWitnessContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        for (String addressBase58 : witness.keySet()) {
            String value = witness.get(addressBase58);
            long count = Long.parseLong(value);
            Contract.VoteWitnessContract.Vote.Builder voteBuilder = Contract.VoteWitnessContract.Vote
                    .newBuilder();
            byte[] address = WalletManager.decodeFromBase58Check(addressBase58);
            if (address == null) {
                continue;
            }
            voteBuilder.setVoteAddress(ByteString.copyFrom(address));
            voteBuilder.setVoteCount(count);
            builder.addVotes(voteBuilder.build());
        }

        return builder.build();
    }

    private static String loadPassword(String walletName) {
        Context context = TronWalletApplication.getAppContext();
        if (context == null || !existWallet(walletName))
            return null;
        SharedPreferences sharedPreferences = context.getSharedPreferences(walletName, Context.MODE_PRIVATE);
        return sharedPreferences.getString(context.getString(R.string.pwd_key), null);
    }

    public static String loadPubKey(String walletName) {
        Context context = TronWalletApplication.getAppContext();
        if (context == null || !existWallet(walletName))
            return null;
        SharedPreferences sharedPreferences = context.getSharedPreferences(walletName, Context.MODE_PRIVATE);
        return sharedPreferences.getString(context.getString(R.string.pub_key), null);
    }

    private static String loadPriKey(String walletName) {
        Context context = TronWalletApplication.getAppContext();
        if (context == null || !existWallet(walletName))
            return null;
        SharedPreferences sharedPreferences = context.getSharedPreferences(walletName, Context.MODE_PRIVATE);
        return sharedPreferences.getString(context.getString(R.string.priv_key), null);
    }

    public static byte[] getPasswordHash(String password) {
        if (!isPasswordValid(password)) {
            return null;
        }
        byte[] pwd;
        pwd = Hash.sha256(password.getBytes());
        pwd = Hash.sha256(pwd);
        pwd = Arrays.copyOfRange(pwd, 0, 16);
        return pwd;
    }

    public static byte[] getEncKey(String password) {
        if (!isPasswordValid(password)) {
            return null;
        }
        byte[] encKey;
        encKey = Hash.sha256(password.getBytes());
        encKey = Arrays.copyOfRange(encKey, 0, 16);
        return encKey;
    }

    public static boolean checkPassword(String walletName, String password) {
        byte[] pwd = getPasswordHash(password);
        if (pwd == null) {
            return false;
        }
        String pwdAsc = ByteArray.toHexString(pwd);
        String pwdInstore = loadPassword(walletName);
        return pwdAsc.equals(pwdInstore);
    }

    public static boolean checkPassword(Wallet wallet, String password) {
        byte[] pwd = getPasswordHash(password);
        if (pwd == null) {
            return false;
        }
        String pwdHexString = ByteArray.toHexString(pwd);
        return pwdHexString.equals(wallet.getEncryptedPassword());
    }

    public static boolean isNameValid(String name) {
        return !name.isEmpty();
    }

    public static boolean isPasswordValid(String password) {
        if (StringUtils.isEmpty(password)) {
            return false;
        }
        if (password.length() < 6) {
            return false;
        }
        if(password.contains("\\s")) {
            return false;
        }
        //Other rule;
        return true;
    }

    public static boolean isAddressValid(byte[] address) {
        if (address == null || address.length == 0) {
            return false;
        }
        if (address.length != CommonConstant.ADDRESS_SIZE) {
            return false;
        }
        byte preFixbyte = address[0];
        if (preFixbyte != CommonConstant.ADD_PRE_FIX_BYTE) {
            return false;
        }

        return true;
    }

    public static String encode58Check(byte[] input) {
        byte[] hash0 = Hash.sha256(input);
        byte[] hash1 = Hash.sha256(hash0);
        byte[] inputCheck = new byte[input.length + 4];
        System.arraycopy(input, 0, inputCheck, 0, input.length);
        System.arraycopy(hash1, 0, inputCheck, input.length, 4);
        return Base58.encode(inputCheck);
    }

    private static byte[] decode58Check(String input) {
        byte[] decodeCheck = Base58.decode(input);
        if (decodeCheck.length <= 4) {
            return null;
        }
        byte[] decodeData = new byte[decodeCheck.length - 4];
        System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
        byte[] hash0 = Hash.sha256(decodeData);
        byte[] hash1 = Hash.sha256(hash0);
        if (hash1[0] == decodeCheck[decodeData.length] &&
                hash1[1] == decodeCheck[decodeData.length + 1] &&
                hash1[2] == decodeCheck[decodeData.length + 2] &&
                hash1[3] == decodeCheck[decodeData.length + 3]) {
            return decodeData;
        }
        return null;
    }

    public static byte[] decodeFromBase58Check(String addressBase58) {
        if (StringUtils.isEmpty(addressBase58)) {
            return null;
        }
        byte[] address = decode58Check(addressBase58);
        if (!isAddressValid(address)) {
            return null;
        }
        return address;
    }

    public static boolean isPrivateKeyValid(String priKey) {
        if (StringUtils.isEmpty(priKey)) {
            return false;
        }
        if (priKey.length() != 64) {
            return false;
        }

        return true;
    }

    public static WitnessList listWitnesses(boolean useSolidity) {
        WitnessList witnessList = rpcCli.listWitnesses(useSolidity);
        if (witnessList != null) {
            List<Witness> list = witnessList.getWitnessesList();
            List<Witness> newList = new ArrayList();
            newList.addAll(list);
            Collections.sort(newList, new WitnessComparator());
            WitnessList.Builder builder = WitnessList.newBuilder();
            for (Witness witness : newList) {
                builder.addWitnesses(witness);
            }
            witnessList = builder.build();
        }
        return witnessList;
    }

    public static AssetIssueList getAssetIssueList(boolean useSolidity) {
        return rpcCli.getAssetIssueList(useSolidity);
    }

    public static NodeList listNodes() {
        return rpcCli.listNodes();
    }

    public static AssetIssueList getAssetIssueByAccount(byte[] address) {
        return rpcCli.getAssetIssueByAccount(address);
    }

    public static AccountNetMessage getAccountNet(byte[] address) {
        return rpcCli.getAccountNet(address);
    }

    public static AssetIssueContract getAssetIssueByName(String assetName) {
        return rpcCli.getAssetIssueByName(assetName);
    }

    public static GrpcAPI.NumberMessage getTotalTransaction() {
        return rpcCli.getTotalTransaction();
    }

    public static GrpcAPI.NumberMessage getNextMaintenanceTime() {
        return rpcCli.getNextMaintenanceTime();
    }

    public static TransactionList getTransactionsFromThis(byte[] address, int offset, int limit) {
        return rpcCli.getTransactionsFromThis(address, offset, limit);
    }

    public static TransactionList getTransactionsToThis(byte[] address, int offset, int limit) {
        return rpcCli.getTransactionsToThis(address, offset, limit);
    }

    public static Transaction getTransactionById(String txID, boolean useSolidity) {
        return rpcCli.getTransactionById(txID, useSolidity);
    }

    public static Protocol.TransactionInfo getTransactionInfoById(String txID) {
        return rpcCli.getTransactionInfo(txID);
    }

    public static Protocol.TransactionInfo getTransactionInfo(Transaction transaction) {
        String txID = Hex.toHexString(Hash.sha256(transaction.getRawData().toByteArray()));
        return rpcCli.getTransactionInfo(txID);
    }

    public static boolean isTransactionConfirmed(Transaction transaction) {
        Protocol.Transaction confirmedTransaction = null;
        String txID = Hex.toHexString(Hash.sha256(transaction.getRawData().toByteArray()));

        int maxTries = 5;
        int tries = 0;
        while ((confirmedTransaction == null || !confirmedTransaction.hasRawData()) && tries <= maxTries) {
            try {
                confirmedTransaction = WalletManager.getTransactionById(txID, true);
                tries++;
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        return confirmedTransaction.hasRawData() && txID.equals(Hex.toHexString(Hash.sha256(confirmedTransaction.getRawData().toByteArray())));
    }

    public static FreezeBalanceContract createFreezeBalanceContract(byte[] owner, long frozen_balance,
                                                                    long frozen_duration) {
        Contract.FreezeBalanceContract.Builder builder = Contract.FreezeBalanceContract.newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(owner);

        builder.setOwnerAddress(byteAddreess).setFrozenBalance(frozen_balance)
                .setFrozenDuration(frozen_duration);

        return builder.build();
    }

    public static Transaction createFreezeBalanceTransaction(byte[] owner, long frozen_balance, long frozen_duration) {
        FreezeBalanceContract contract = createFreezeBalanceContract(owner, frozen_balance, frozen_duration);
        return rpcCli.createTransaction(contract);
    }

    public static Transaction createUnfreezeBalanceTransaction(byte[] owner) {
        UnfreezeBalanceContract contract = createUnfreezeBalanceContract(owner);
        return rpcCli.createTransaction(contract);
    }

    public static UnfreezeBalanceContract createUnfreezeBalanceContract(byte[] owner) {

        Contract.UnfreezeBalanceContract.Builder builder = Contract.UnfreezeBalanceContract
                .newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(owner);

        builder.setOwnerAddress(byteAddreess);

        return builder.build();
    }

    public static Block getBlockById(String blockID) {
        return rpcCli.getBlockById(blockID);
    }

    public static BlockList getBlockByLimitNext(long start, long end) {
        return rpcCli.getBlockByLimitNext(start, end);
    }

    public static BlockList getBlockByLatestNum(long num) {
        return rpcCli.getBlockByLatestNum(num);
    }

}
