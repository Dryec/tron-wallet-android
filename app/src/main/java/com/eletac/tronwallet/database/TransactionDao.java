package com.eletac.tronwallet.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    void insert(Transaction transaction);

    @Query("DELETE FROM transactions")
    void deleteAll();

    @Query("SELECT * from transactions")
    List<Transaction> getAllTransactions();

    @Query("SELECT * from transactions WHERE sender_address = :sender")
    List<Transaction> getAllTransactionsFromSender(String sender);
}
