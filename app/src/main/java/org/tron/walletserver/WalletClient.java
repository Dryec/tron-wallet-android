package org.tron.walletserver;

import android.content.Context;
import android.content.SharedPreferences;

import com.eletac.tronwallet.R;
import com.eletac.tronwallet.TronWalletApplication;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.*;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Hash;
import org.tron.common.crypto.SymmEncoder;
import org.tron.common.utils.*;
import org.tron.core.config.Configuration;
import org.tron.core.config.Parameter.CommonConstant;
import org.tron.protos.Contract;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Contract.FreezeBalanceContract;
import org.tron.protos.Contract.UnfreezeBalanceContract;
import org.tron.protos.Contract.WithdrawBalanceContract;
import org.tron.protos.Protocol.*;

import java.math.BigInteger;
import java.util.*;

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

public class WalletClient {

  private static final Logger logger = LoggerFactory.getLogger("WalletClient");
  private ECKey ecKey = null;
  private boolean loginState = false;

  private static GrpcClient rpcCli;

  public static void init() {
      if(rpcCli != null) {
          try {
              rpcCli.shutdown();
          } catch (InterruptedException e) {
              e.printStackTrace();
          }
      }

      Context context = TronWalletApplication.getAppContext();
      SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

      String ip = sharedPreferences.getString(context.getString(R.string.ip_key), context.getString(R.string.fullnode_ip));
      int port = sharedPreferences.getInt(context.getString(R.string.port_key), Integer.parseInt(context.getString(R.string.fullnode_port)));

      rpcCli = new GrpcClient(String.format(Locale.US, "%s:%d", ip, port), "");
  }

  /**
   * Creates a new WalletClient with a random ECKey or no ECKey.
   */
  public WalletClient(boolean genEcKey) {
    if (genEcKey) {
      this.ecKey = new ECKey(Utils.getRandom());
    }
  }

  //  Create Wallet with a pritKey
  public WalletClient(String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    this.ecKey = temKey;
  }

  public boolean login(String password) {
    loginState = checkPassWord(password);
    return loginState;
  }

  public boolean isLoginState() {
    return loginState;
  }

  public void logout() {
    loginState = false;
  }

  /**
   * Get a Wallet from storage
   */
  public static WalletClient GetWalletByStorage(String password) {
    String priKeyEnced = loadPriKey();
    if (priKeyEnced == null) {
      return null;
    }
    //dec priKey
    byte[] priKeyAscEnced = priKeyEnced.getBytes();
    byte[] priKeyHexEnced = Hex.decode(priKeyAscEnced);
    byte[] aesKey = getEncKey(password);
    byte[] priKeyHexPlain = SymmEncoder.AES128EcbDec(priKeyHexEnced, aesKey);
    String priKeyPlain = Hex.toHexString(priKeyHexPlain);

    return new WalletClient(priKeyPlain);
  }

  /**
   * Creates a Wallet with an existing ECKey.
   */

  public WalletClient(final ECKey ecKey) {
    this.ecKey = ecKey;
  }

  public ECKey getEcKey() {
    return ecKey;
  }

  public byte[] getAddress() {
    return ecKey.getAddress();
  }

  public void store(String password) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      logger.warn("Warning: Store wallet failed, PrivKey is null !!");
      return;
    }
    byte[] pwd = getPassWord(password);
    String pwdAsc = ByteArray.toHexString(pwd);
    byte[] privKeyPlain = ecKey.getPrivKeyBytes();
    System.out.println("privKey:" + ByteArray.toHexString(privKeyPlain));
    //encrypted by password
    byte[] aseKey = getEncKey(password);
    byte[] privKeyEnced = SymmEncoder.AES128EcbEnc(privKeyPlain, aseKey);
    String privKeyStr = ByteArray.toHexString(privKeyEnced);
    byte[] pubKeyBytes = ecKey.getPubKey();
    String pubKeyStr = ByteArray.toHexString(pubKeyBytes);
    
    Context context = TronWalletApplication.getAppContext();
    if(context == null)
      return;
    SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = sharedPreferences.edit();

    // SAVE PASSWORD
    editor.putString(context.getString(R.string.pwd_key), pwdAsc);
    // SAVE PUBKEY
    editor.putString(context.getString(R.string.pub_key), pubKeyStr);
    // SAVE PRIKEY
    editor.putString(context.getString(R.string.priv_key), privKeyStr);

    editor.commit();
  }

  public Account queryAccount() {
    byte[] address;
    if (this.ecKey == null) {
      String pubKey = loadPubKey(); //04 PubKey[128]
      if (StringUtils.isEmpty(pubKey)) {
        logger.warn("Warning: QueryAccount failed, no wallet address !!");
        return null;
      }
      byte[] pubKeyAsc = pubKey.getBytes();
      byte[] pubKeyHex = Hex.decode(pubKeyAsc);
      this.ecKey = ECKey.fromPublicOnly(pubKeyHex);
    }
    return queryAccount(getAddress());
  }

  public static Account queryAccount(byte[] address) {
    return rpcCli.queryAccount(address);//call rpc
  }

  private Transaction signTransaction(Transaction transaction) {
    if (this.ecKey == null || this.ecKey.getPrivKey() == null) {
      logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, this.ecKey);
  }

  public boolean sendCoin(byte[] to, long amount) {
    byte[] owner = getAddress();
    Contract.TransferContract contract = createTransferContract(to, owner, amount);
    Transaction transaction = rpcCli.createTransaction(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(transaction);
    System.out.println("--------------------------------");
    System.out.println(
        "txid = " + ByteArray.toHexString(Hash.sha256(transaction.getRawData().toByteArray())));
    System.out.println("--------------------------------");
    return rpcCli.broadcastTransaction(transaction);
  }

  public boolean updateAccount(byte[] addressBytes, byte[] accountNameBytes) {
    Contract.AccountUpdateContract contract = createAccountUpdateContract(accountNameBytes,
        addressBytes);
    Transaction transaction = rpcCli.createTransaction(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = signTransaction(transaction);
    return rpcCli.broadcastTransaction(transaction);
  }

  public boolean transferAsset(byte[] to, byte[] assertName, long amount) {
    byte[] owner = getAddress();
    Transaction transaction = createTransferAssetTransaction(to, assertName, owner, amount);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(transaction);
    return rpcCli.broadcastTransaction(transaction);
  }

  public static Transaction createTransferAssetTransaction(byte[] to, byte[] assertName,
      byte[] owner, long amount) {
    Contract.TransferAssetContract contract = createTransferAssetContract(to, assertName, owner,
        amount);
    return rpcCli.createTransferAssetTransaction(contract);
  }

  public boolean participateAssetIssue(byte[] to, byte[] assertName, long amount) {
    byte[] owner = getAddress();
    Transaction transaction = participateAssetIssueTransaction(to, assertName, owner, amount);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(transaction);
    return rpcCli.broadcastTransaction(transaction);
  }

  public static Transaction participateAssetIssueTransaction(byte[] to, byte[] assertName,
      byte[] owner, long amount) {
    Contract.ParticipateAssetIssueContract contract = participateAssetIssueContract(to, assertName,
        owner, amount);
    return rpcCli.createParticipateAssetIssueTransaction(contract);
  }

  public static Transaction updateAccountTransaction(byte[] addressBytes, byte[] accountNameBytes) {
    Contract.AccountUpdateContract contract = createAccountUpdateContract(accountNameBytes,
        addressBytes);
    return rpcCli.createTransaction(contract);
  }

  public static boolean broadcastTransaction(byte[] transactionBytes)
      throws InvalidProtocolBufferException {
    Transaction transaction = Transaction.parseFrom(transactionBytes);
    if (false == TransactionUtils.validTransaction(transaction)) {
      return false;
    }
    return rpcCli.broadcastTransaction(transaction);
  }

  public boolean createAssetIssue(Contract.AssetIssueContract contract) {
    Transaction transaction = rpcCli.createAssetIssue(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(transaction);
    return rpcCli.broadcastTransaction(transaction);
  }

  public boolean createWitness(byte[] url) {
    byte[] owner = getAddress();
    Transaction transaction = createWitnessTransaction(owner, url);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(transaction);
    return rpcCli.broadcastTransaction(transaction);
  }

  public static Transaction createWitnessTransaction(byte[] owner, byte[] url) {
    Contract.WitnessCreateContract contract = createWitnessCreateContract(owner, url);
    return rpcCli.createWitness(contract);
  }


  public static Transaction createVoteWitnessTransaction(byte[] owner,
      HashMap<String, String> witness) {
    Contract.VoteWitnessContract contract = createVoteWitnessContract(owner, witness);
    return rpcCli.voteWitnessAccount(contract);
  }



  public static Transaction createAssetIssueTransaction(Contract.AssetIssueContract contract) {
    return rpcCli.createAssetIssue(contract);
  }

  public static Block GetBlock(long blockNum) {
    return rpcCli.getBlock(blockNum);
  }

  public boolean voteWitness(HashMap<String, String> witness) {
    byte[] owner = getAddress();
    Contract.VoteWitnessContract contract = createVoteWitnessContract(owner, witness);
    Transaction transaction = rpcCli.voteWitnessAccount(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(transaction);
    return rpcCli.broadcastTransaction(transaction);
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

  public static Contract.ParticipateAssetIssueContract participateAssetIssueContract(byte[] to,
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

  public static Contract.AccountCreateContract createAccountCreateContract(AccountType accountType,
      byte[] accountName, byte[] address) {
    Contract.AccountCreateContract.Builder builder = Contract.AccountCreateContract.newBuilder();
    ByteString bsaAdress = ByteString.copyFrom(address);
    ByteString bsAccountName = ByteString.copyFrom(accountName);
    builder.setType(accountType);
    builder.setAccountName(bsAccountName);
    builder.setOwnerAddress(bsaAdress);

    return builder.build();
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

  public static Contract.WitnessCreateContract createWitnessCreateContract(byte[] owner,
      byte[] url) {
    Contract.WitnessCreateContract.Builder builder = Contract.WitnessCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setUrl(ByteString.copyFrom(url));

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
      byte[] address = WalletClient.decodeFromBase58Check(addressBase58);
      if (address == null) {
        continue;
      }
      voteBuilder.setVoteAddress(ByteString.copyFrom(address));
      voteBuilder.setVoteCount(count);
      builder.addVotes(voteBuilder.build());
    }

    return builder.build();
  }

  private static String loadPassword() {
    Context context = TronWalletApplication.getAppContext();
    if(context == null)
      return null;
    SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
    return sharedPreferences.getString(context.getString(R.string.pwd_key), null);
  }

  public static String loadPubKey() {
    Context context = TronWalletApplication.getAppContext();
    if(context == null)
      return null;
    SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
    return sharedPreferences.getString(context.getString(R.string.pub_key), null);
  }

  private static String loadPriKey() {
    Context context = TronWalletApplication.getAppContext();
    if(context == null)
      return null;
    SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
    return sharedPreferences.getString(context.getString(R.string.priv_key), null);
  }

  /**
   * Get a Wallet from storage
   */
  public static WalletClient GetWalletByStorageIgnorPrivKey() {
    try {
      String pubKey = loadPubKey(); //04 PubKey[128]
      if (StringUtils.isEmpty(pubKey)) {
        return null;
      }
      byte[] pubKeyAsc = pubKey.getBytes();
      byte[] pubKeyHex = Hex.decode(pubKeyAsc);
      ECKey eccKey = ECKey.fromPublicOnly(pubKeyHex);
      return new WalletClient(eccKey);
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  public static String getAddressByStorage() {
    try {
      String pubKey = loadPubKey(); //04 PubKey[128]
      if (StringUtils.isEmpty(pubKey)) {
        return null;
      }
      byte[] pubKeyAsc = pubKey.getBytes();
      byte[] pubKeyHex = Hex.decode(pubKeyAsc);
      ECKey eccKey = ECKey.fromPublicOnly(pubKeyHex);
      return ByteArray.toHexString(eccKey.getAddress());
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  public static byte[] getPassWord(String password) {
    if (!passwordValid(password)) {
      return null;
    }
    byte[] pwd;
    pwd = Hash.sha256(password.getBytes());
    pwd = Hash.sha256(pwd);
    pwd = Arrays.copyOfRange(pwd, 0, 16);
    return pwd;
  }

  public static byte[] getEncKey(String password) {
    if (!passwordValid(password)) {
      return null;
    }
    byte[] encKey;
    encKey = Hash.sha256(password.getBytes());
    encKey = Arrays.copyOfRange(encKey, 0, 16);
    return encKey;
  }

  public static boolean checkPassWord(String password) {
    byte[] pwd = getPassWord(password);
    if (pwd == null) {
      return false;
    }
    String pwdAsc = ByteArray.toHexString(pwd);
    String pwdInstore = loadPassword();
    return pwdAsc.equals(pwdInstore);
  }

  public static boolean passwordValid(String password) {
    if (StringUtils.isEmpty(password)) {
      logger.warn("Warning: Password is empty !!");
      return false;
    }
    if (password.length() < 6) {
      logger.warn("Warning: Password is too short !!");
      return false;
    }
    //Other rule;
    return true;
  }

  public static boolean addressValid(byte[] address) {
    if (address == null || address.length == 0) {
      logger.warn("Warning: Address is empty !!");
      return false;
    }
    if (address.length != CommonConstant.ADDRESS_SIZE) {
      logger.warn(
          "Warning: Address length need " + CommonConstant.ADDRESS_SIZE + " but " + address.length
              + " !!");
      return false;
    }
    byte preFixbyte = address[0];
    if (preFixbyte != CommonConstant.ADD_PRE_FIX_BYTE) {
      logger.warn("Warning: Address need prefix with " + CommonConstant.ADD_PRE_FIX_BYTE + " but "
          + preFixbyte + " !!");
      return false;
    }
    //Other rule;
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
      logger.warn("Warning: Address is empty !!");
      return null;
    }
    if (addressBase58.length() != CommonConstant.BASE58CHECK_ADDRESS_SIZE) {
      logger.warn(
          "Warning: Base58 address length need " + CommonConstant.BASE58CHECK_ADDRESS_SIZE + " but "
              + addressBase58.length()
              + " !!");
      return null;
    }
    byte[] address = decode58Check(addressBase58);
    if (!addressValid(address)) {
      return null;
    }
    return address;
  }

  public static boolean priKeyValid(String priKey) {
    if (StringUtils.isEmpty(priKey)) {
      logger.warn("Warning: PrivateKey is empty !!");
      return false;
    }
    if (priKey.length() != 64) {
      logger.warn("Warning: PrivateKey length need 64 but " + priKey.length() + " !!");
      return false;
    }
    //Other rule;
    return true;
  }

  public static AccountList listAccounts() {
    AccountList result = rpcCli.listAccounts();
    if (result != null) {
      List<Account> list = result.getAccountsList();
      List<Account> newList = new ArrayList();
      newList.addAll(list);
      Collections.sort(newList, new AccountComparator());
      AccountList.Builder builder = AccountList.newBuilder();
      for(Account account : newList) {
        builder.addAccounts(account);
      }
      result = builder.build();
    }
    return result;
  }

  public static WitnessList listWitnesses() {
    WitnessList witnessList = rpcCli.listWitnesses();
    if (witnessList != null) {
      List<Witness> list = witnessList.getWitnessesList();
      List<Witness> newList = new ArrayList();
      newList.addAll(list);
      Collections.sort(newList, new WitnessComparator());
      WitnessList.Builder builder = WitnessList.newBuilder();
      for(Witness witness : newList) {
        builder.addWitnesses(witness);
      }
      witnessList = builder.build();
    }
    return witnessList;
  }

  public static AssetIssueList getAssetIssueListByTimestamp(long timestamp) {
    return rpcCli.getAssetIssueListByTimestamp(timestamp);
  }

  public static TransactionList getTransactionsByTimestamp(long start, long end) {
    return rpcCli.getTransactionsByTimestamp(start, end);
  }

  public static AssetIssueList getAssetIssueList() {
    return rpcCli.getAssetIssueList();
  }

  public static NodeList listNodes() {
    return rpcCli.listNodes();
  }

  public static AssetIssueList getAssetIssueByAccount(byte[] address) {
    return rpcCli.getAssetIssueByAccount(address);
  }

  public static AssetIssueContract getAssetIssueByName(String assetName) {
    return rpcCli.getAssetIssueByName(assetName);
  }

  public static GrpcAPI.NumberMessage getTotalTransaction() {
    return rpcCli.getTotalTransaction();
  }

  public static TransactionList getTransactionsFromThis(byte[] address) {
    return rpcCli.getTransactionsFromThis(address);
  }

  public static TransactionList getTransactionsToThis(byte[] address) {
    return rpcCli.getTransactionsToThis(address);
  }

  public static Transaction getTransactionById(String txID) {
    return rpcCli.getTransactionById(txID);
  }

  public boolean freezeBalance(long frozen_balance, long frozen_duration) {

    Contract.FreezeBalanceContract contract = createFreezeBalanceContract(getAddress(), frozen_balance,
        frozen_duration);

    Transaction transaction = rpcCli.createTransaction(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = signTransaction(transaction);
    return rpcCli.broadcastTransaction(transaction);
  }

  public static Transaction createFreezeBalanceTransaction(byte[] owner, long frozen_balance, long frozen_duration) {
      FreezeBalanceContract contract = createFreezeBalanceContract(owner, frozen_balance, frozen_duration);
      return rpcCli.createTransaction(contract);
  }

  public static FreezeBalanceContract createFreezeBalanceContract(byte[] owner, long frozen_balance,
      long frozen_duration) {
    Contract.FreezeBalanceContract.Builder builder = Contract.FreezeBalanceContract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(owner);

    builder.setOwnerAddress(byteAddreess).setFrozenBalance(frozen_balance)
        .setFrozenDuration(frozen_duration);

    return builder.build();
  }

  public boolean unfreezeBalance() {
    Contract.UnfreezeBalanceContract contract = createUnfreezeBalanceContract(getAddress());

    Transaction transaction = rpcCli.createTransaction(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = signTransaction(transaction);
    return rpcCli.broadcastTransaction(transaction);
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

  public boolean withdrawBalance() {
    Contract.WithdrawBalanceContract contract = createWithdrawBalanceContract();

    Transaction transaction = rpcCli.createTransaction(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = signTransaction(transaction);
    return rpcCli.broadcastTransaction(transaction);
  }

  private WithdrawBalanceContract createWithdrawBalanceContract() {

    byte[] address = getAddress();
    Contract.WithdrawBalanceContract.Builder builder = Contract.WithdrawBalanceContract
        .newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);

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
