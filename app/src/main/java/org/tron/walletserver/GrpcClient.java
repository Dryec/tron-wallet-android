package org.tron.walletserver;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountNetMessage;
import org.tron.api.GrpcAPI.AccountPaginated;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.BlockLimit;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.NodeList;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.api.WalletExtensionGrpc;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;

public class GrpcClient {

  private static final Logger logger = LoggerFactory.getLogger("GrpcClient");
  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private WalletExtensionGrpc.WalletExtensionBlockingStub blockingStubExtension = null;


  public GrpcClient(String fullnode, String soliditynode) {
    if (!StringUtils.isEmpty(fullnode)) {
      channelFull = ManagedChannelBuilder.forTarget(fullnode)
          .usePlaintext(true)
          .build();
      blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    }
    if (!StringUtils.isEmpty(soliditynode)) {
      channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
          .usePlaintext(true)
          .build();
      blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
      blockingStubExtension = WalletExtensionGrpc.newBlockingStub(channelSolidity);
    }
  }

  public void shutdown() {
    if (channelFull != null) {
      channelFull.shutdown();
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown();
    }
  }

  public Account queryAccount(byte[] address, boolean useSolidity) {
    ByteString addressBS = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBS).build();
    if (blockingStubSolidity != null && useSolidity) {
      return blockingStubSolidity.getAccount(request);
    } else {
      return blockingStubFull.getAccount(request);
    }
  }

  public GrpcAPI.TransactionExtention createTransaction(Contract.AccountUpdateContract contract) {
    return blockingStubFull.updateAccount2(contract);
  }

  public GrpcAPI.TransactionExtention createTransaction(Contract.UpdateAssetContract contract) {
    return blockingStubFull.updateAsset2(contract);
  }

  public GrpcAPI.TransactionExtention createTransaction(Contract.TransferContract contract) {
    return blockingStubFull.createTransaction2(contract);
  }

  public GrpcAPI.TransactionExtention createTransaction(Contract.FreezeBalanceContract contract) {
    return blockingStubFull.freezeBalance2(contract);
  }

  public GrpcAPI.TransactionExtention createTransaction(Contract.WithdrawBalanceContract contract) {
    return blockingStubFull.withdrawBalance2(contract);
  }

  public GrpcAPI.TransactionExtention createTransaction(Contract.UnfreezeBalanceContract contract) {
    return blockingStubFull.unfreezeBalance2(contract);
  }

  public GrpcAPI.TransactionExtention createTransaction(Contract.UnfreezeAssetContract contract) {
    return blockingStubFull.unfreezeAsset2(contract);
  }

  public GrpcAPI.TransactionExtention createTransferAssetTransaction(Contract.TransferAssetContract contract) {
    return blockingStubFull.transferAsset2(contract);
  }

  public GrpcAPI.TransactionExtention createParticipateAssetIssueTransaction(
      Contract.ParticipateAssetIssueContract contract) {
    return blockingStubFull.participateAssetIssue2(contract);
  }

  public GrpcAPI.TransactionExtention createAssetIssue(Contract.AssetIssueContract contract) {
    return blockingStubFull.createAssetIssue2(contract);
  }

  public GrpcAPI.TransactionExtention voteWitnessAccount(Contract.VoteWitnessContract contract) {
    return blockingStubFull.voteWitnessAccount2(contract);
  }

  public GrpcAPI.TransactionExtention createWitness(Contract.WitnessCreateContract contract) {
    return blockingStubFull.createWitness2(contract);
  }

  public GrpcAPI.TransactionExtention updateWitness(Contract.WitnessUpdateContract contract) {
    return blockingStubFull.updateWitness2(contract);
  }

  public boolean broadcastTransaction(Transaction signaturedTransaction) {
    int i = 10;
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(signaturedTransaction);
    while (response.getResult() == false && response.getCode() == response_code.SERVER_BUSY
        && i > 0) {
      i--;
      response = blockingStubFull.broadcastTransaction(signaturedTransaction);
      logger.info("repeate times = " + (11 - i));
      try {
        Thread.sleep(300);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    if (response.getResult() == false) {
      logger.info("Code = " + response.getCode());
      logger.info("Message = " + response.getMessage().toStringUtf8());
    }
    return response.getResult();
  }

  public GrpcAPI.BlockExtention getBlock(long blockNum, boolean useSolidity) {
    if (blockNum < 0) {
      if (blockingStubSolidity != null && useSolidity) {
        return blockingStubSolidity.getNowBlock2(EmptyMessage.newBuilder().build());
      } else {
        return blockingStubFull.getNowBlock2(EmptyMessage.newBuilder().build());
      }
    }
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    if (blockingStubSolidity != null) {
      return blockingStubSolidity.getBlockByNum2(builder.build());
    } else {
      return blockingStubFull.getBlockByNum2(builder.build());
    }
  }

//  public Optional<AccountList> listAccounts() {
//    AccountList accountList = blockingStubSolidity
//        .listAccounts(EmptyMessage.newBuilder().build());
//    return Optional.ofNullable(accountList);
//
//  }

  public WitnessList listWitnesses(boolean useSolidity) {
    if (blockingStubSolidity != null && useSolidity) {
      WitnessList witnessList = blockingStubSolidity.listWitnesses(EmptyMessage.newBuilder().build());
      return (witnessList);
    } else {
      WitnessList witnessList = blockingStubFull.listWitnesses(EmptyMessage.newBuilder().build());
      return (witnessList);
    }
  }

  public AssetIssueList getAssetIssueList(boolean useSolidity) {
    if (blockingStubSolidity != null && useSolidity) {
      AssetIssueList assetIssueList = blockingStubSolidity
          .getAssetIssueList(EmptyMessage.newBuilder().build());
      return (assetIssueList);
    } else {
      AssetIssueList assetIssueList = blockingStubFull
          .getAssetIssueList(EmptyMessage.newBuilder().build());
      return (assetIssueList);
    }
  }

  public NodeList listNodes() {
    NodeList nodeList = blockingStubFull.listNodes(EmptyMessage.newBuilder().build());
    return (nodeList);
  }

  public AssetIssueList getAssetIssueByAccount(byte[] address) {
    ByteString addressBS = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBS).build();
    AssetIssueList assetIssueList = blockingStubFull.getAssetIssueByAccount(request);
    return (assetIssueList);
  }

  public AccountNetMessage getAccountNet(byte[] address) {
    ByteString addressBS = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBS).build();
    return blockingStubFull.getAccountNet(request);
  }

  public GrpcAPI.AccountResourceMessage getAccountRes(byte[] address) {
    ByteString addressBS = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBS).build();
    return blockingStubFull.getAccountResource(request);
  }

  public Contract.AssetIssueContract getAssetIssueByName(String assetName) {
    ByteString assetNameBs = ByteString.copyFrom(assetName.getBytes());
    BytesMessage request = BytesMessage.newBuilder().setValue(assetNameBs).build();
    return blockingStubFull.getAssetIssueByName(request);
  }

  public NumberMessage getTotalTransaction() {
    return blockingStubFull.totalTransaction(EmptyMessage.newBuilder().build());
  }

  public NumberMessage getNextMaintenanceTime() {
    return blockingStubFull.getNextMaintenanceTime(EmptyMessage.newBuilder().build());
  }

  public GrpcAPI.TransactionListExtention getTransactionsFromThis(byte[] address, int offset, int limit) {
    ByteString addressBS = ByteString.copyFrom(address);
    Account account = Account.newBuilder().setAddress(addressBS).build();
    AccountPaginated.Builder accountPaginated = AccountPaginated.newBuilder();
    accountPaginated.setAccount(account);
    accountPaginated.setOffset(offset);
    accountPaginated.setLimit(limit);
    GrpcAPI.TransactionListExtention transactionList = blockingStubExtension
        .getTransactionsFromThis2(accountPaginated.build());
    return (transactionList);
  }

  public GrpcAPI.TransactionListExtention getTransactionsToThis(byte[] address, int offset, int limit) {
    ByteString addressBS = ByteString.copyFrom(address);
    Account account = Account.newBuilder().setAddress(addressBS).build();
    AccountPaginated.Builder accountPaginated = AccountPaginated.newBuilder();
    accountPaginated.setAccount(account);
    accountPaginated.setOffset(offset);
    accountPaginated.setLimit(limit);
    GrpcAPI.TransactionListExtention transactionList = blockingStubExtension
        .getTransactionsToThis2(accountPaginated.build());
    return (transactionList);
  }

  public Transaction getTransactionById(String txID, boolean useSolidity) {
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txID));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    if (blockingStubSolidity != null && useSolidity) {
      Transaction transaction = blockingStubSolidity.getTransactionById(request);
      return (transaction);
    } else {
      Transaction transaction = blockingStubFull.getTransactionById(request);
      return (transaction);
    }
  }

  public Block getBlockById(String blockID) {
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(blockID));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    Block block = blockingStubFull.getBlockById(request);
    return (block);
  }

  public GrpcAPI.BlockListExtention getBlockByLimitNext(long start, long end) {
    BlockLimit.Builder builder = BlockLimit.newBuilder();
    builder.setStartNum(start);
    builder.setEndNum(end);
    GrpcAPI.BlockListExtention blockList = blockingStubFull.getBlockByLimitNext2(builder.build());
    return (blockList);
  }

  public GrpcAPI.BlockListExtention getBlockByLatestNum(long num) {
    NumberMessage numberMessage = NumberMessage.newBuilder().setNum(num).build();
    GrpcAPI.BlockListExtention blockList = blockingStubFull.getBlockByLatestNum2(numberMessage);
    return (blockList);
  }

  public Protocol.TransactionInfo getTransactionInfo(String txID) {
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txID));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    if (blockingStubSolidity != null) {
      return blockingStubSolidity.getTransactionInfoById(request);
    }
    return Protocol.TransactionInfo.getDefaultInstance();
  }
}
