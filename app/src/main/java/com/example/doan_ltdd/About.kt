package com.example.doan_ltdd

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val btnBack = findViewById<ImageView>(R.id.btnBackAbout)
        btnBack.setOnClickListener {
            finish() // Đóng activity quay về màn hình trước
        }
    }
}