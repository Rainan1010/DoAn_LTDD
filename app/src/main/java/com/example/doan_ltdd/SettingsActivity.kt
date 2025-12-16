package com.example.doan_ltdd

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class SettingsActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var cardGlobalTime: MaterialCardView
    private lateinit var tvGlobalTime: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        btnBack = findViewById(R.id.btnBackSettings)
        cardGlobalTime = findViewById(R.id.cardGlobalTime)
        tvGlobalTime = findViewById(R.id.tvGlobalTime)

        btnBack.setOnClickListener { finish() }

        // Load giờ đã lưu
        loadSavedTime()

        // Sự kiện chọn giờ
        cardGlobalTime.setOnClickListener {
            showTimePicker()
        }
    }

    private fun loadSavedTime() {
        val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val hour = sharedPref.getInt("global_hour", 9) // Mặc định 9h
        val minute = sharedPref.getInt("global_minute", 0)
        tvGlobalTime.text = String.format("%02d:%02d", hour, minute)
    }

    private fun showTimePicker() {
        val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val currentHour = sharedPref.getInt("global_hour", 9)
        val currentMinute = sharedPref.getInt("global_minute", 0)

        TimePickerDialog(this, { _, hourOfDay, minute ->
            // Lưu vào SharedPreferences
            with(sharedPref.edit()) {
                putInt("global_hour", hourOfDay)
                putInt("global_minute", minute)
                apply()
            }
            // Cập nhật UI
            tvGlobalTime.text = String.format("%02d:%02d", hourOfDay, minute)
        }, currentHour, currentMinute, true).show()
    }
}