package com.example.doan_ltdd

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [SavingsGoal::class, DepositLog::class, NotificationLog::class, Category::class], version = 3, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun savingsDao(): SavingsDAO
    abstract fun notificationDao(): NotificationDAO
    abstract fun categoryDao(): CategoryDAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "savings_app_database"
                )
                    .fallbackToDestructiveMigration() // Xóa DB cũ nếu version thay đổi (chỉ dùng lúc dev)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}