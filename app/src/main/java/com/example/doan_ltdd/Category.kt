package com.example.doan_ltdd

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val name: String // Dùng tên làm khóa chính luôn (ví dụ: "Mua sắm", "Du lịch")
)