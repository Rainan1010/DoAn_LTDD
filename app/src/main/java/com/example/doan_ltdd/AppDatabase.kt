package com.example.doan_ltdd

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// [SỬA Ở ĐÂY] Thêm User::class và đổi version = 4
@Database(entities = [SavingsGoal::class, DepositLog::class, NotificationLog::class, Category::class, User::class], version = 4, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun savingsDao(): SavingsDAO
    abstract fun notificationDao(): NotificationDAO
    abstract fun categoryDao(): CategoryDAO
    abstract fun userDao(): UserDAO

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
                    .fallbackToDestructiveMigration() // Dòng này sẽ xóa dữ liệu cũ để tạo bảng Users mới
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}