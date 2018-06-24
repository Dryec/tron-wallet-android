package com.eletac.tronwallet.database;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.arch.persistence.room.migration.Migration;

@Database(entities = {Transaction.class}, version = 1)
@TypeConverters({Converters.class})
public abstract class TronWalletDatabase extends RoomDatabase {

    public abstract TransactionDao transactionDao();

    public static final Migration MIGRATION_1_2 = new Migration(1,2){
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Since we didn't alter the table, there's nothing else to do here.
        }
    };
}
