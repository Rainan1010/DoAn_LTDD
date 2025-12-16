package com.example.doan_ltdd

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class NotificationActivity : AppCompatActivity() {

    // Khai báo các view là biến toàn cục để dùng chung cho cả setControl và setEvent
    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvNotifications: RecyclerView
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        setControl()
        setEvent()
    }

    // Hàm ánh xạ View và khởi tạo cấu hình ban đầu
    private fun setControl() {
        toolbar = findViewById(R.id.toolbarNoti)
        rvNotifications = findViewById(R.id.rvNotifications)

        // Cấu hình RecyclerView và Adapter
        adapter = NotificationAdapter()
        rvNotifications.layoutManager = LinearLayoutManager(this)
        rvNotifications.adapter = adapter
    }

    // Hàm xử lý các sự kiện (Click, Load data, Logic...)
    private fun setEvent() {
        // 1. Setup Toolbar
        setSupportActionBar(toolbar)
        // Quan trọng: Tắt tiêu đề mặc định để không bị trùng với TextView của bạn
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Sự kiện click nút Back trên toolbar
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // 2. Load dữ liệu từ Room Database
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            db.notificationDao().getAllNotifications().collect { list ->
                adapter.submitList(list)
            }
        }
    }
}