package com.example.doan_ltdd

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class StatisticsActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var progressTotal: CircularProgressIndicator
    private lateinit var tvTotalPercent: TextView
    private lateinit var tvTotalCurrent: TextView
    private lateinit var tvTotalTarget: TextView
    private lateinit var rvCategoryStats: RecyclerView

    private lateinit var database: AppDatabase
    private lateinit var adapter: CategoryStatsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        database = AppDatabase.getDatabase(this)

        setControl()
        setEvent()
        loadData()
    }

    private fun setControl() {
        btnBack = findViewById(R.id.btnBackStats)
        progressTotal = findViewById(R.id.progressTotal)
        tvTotalPercent = findViewById(R.id.tvTotalPercent)
        tvTotalCurrent = findViewById(R.id.tvTotalCurrent)
        tvTotalTarget = findViewById(R.id.tvTotalTarget)
        rvCategoryStats = findViewById(R.id.rvCategoryStats)

        // Setup RecyclerView
        adapter = CategoryStatsAdapter()
        rvCategoryStats.layoutManager = LinearLayoutManager(this)
        rvCategoryStats.adapter = adapter
    }

    private fun setEvent() {
        btnBack.setOnClickListener { finish() }
    }

    private fun loadData() {
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

        lifecycleScope.launch {
            // Lấy toàn bộ mục tiêu từ DB
            database.savingsDao().getAllGoals().collect { goals ->

                // 1. TÍNH TOÁN THỐNG KÊ TỔNG
                val totalCurrent = goals.sumOf { it.currentAmount }
                val totalTarget = goals.sumOf { it.targetAmount }

                val totalProgress = if (totalTarget > 0) {
                    ((totalCurrent / totalTarget) * 100).toInt()
                } else 0

                // Update UI phần tổng
                progressTotal.setProgress(totalProgress, true) // animation
                tvTotalPercent.text = "$totalProgress%"
                tvTotalCurrent.text = formatter.format(totalCurrent)
                tvTotalTarget.text = formatter.format(totalTarget)

                // 2. TÍNH TOÁN THỐNG KÊ THEO DANH MỤC
                // Gom nhóm các goal có cùng category lại với nhau
                val groupedMap = goals.groupBy { it.category }

                val categoryStatsList = mutableListOf<CategoryStat>()

                // Duyệt qua từng nhóm để tính tổng
                for ((categoryName, goalsInGroup) in groupedMap) {
                    val groupCurrent = goalsInGroup.sumOf { it.currentAmount }
                    val groupTarget = goalsInGroup.sumOf { it.targetAmount }

                    categoryStatsList.add(CategoryStat(categoryName, groupCurrent, groupTarget))
                }

                // Sắp xếp theo số tiền đã tiết kiệm giảm dần (để đẹp hơn)
                categoryStatsList.sortByDescending { it.totalCurrent }

                // Update UI phần danh sách
                adapter.submitList(categoryStatsList)
            }
        }
    }
}