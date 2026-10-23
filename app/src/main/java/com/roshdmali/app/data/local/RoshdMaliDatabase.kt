package com.roshdmali.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class RoshdMaliDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var instance: RoshdMaliDatabase? = null

        fun getInstance(context: Context): RoshdMaliDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RoshdMaliDatabase::class.java,
                    "roshd-mali.db",
                ).build().also { instance = it }
            }
    }
}