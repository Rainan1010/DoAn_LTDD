package com.example.doan_ltdd

import androidx.room.*

@Dao
interface UserDAO {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun registerUser(user: User)

    @Query("SELECT * FROM users WHERE username = :username AND password = :password")
    suspend fun login(username: String, password: String): User?

    @Query("SELECT COUNT(*) FROM users WHERE username = :username")
    suspend fun checkUserExists(username: String): Int

    // [MỚI] Lấy thông tin user theo username
    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): User?

    // [MỚI] Cập nhật thông tin user
    @Update
    suspend fun updateUser(user: User)
}