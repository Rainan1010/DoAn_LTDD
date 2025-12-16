package com.example.doan_ltdd

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() { // Đổi tên class thành HistoryActivity cho chuẩn

    private lateinit var tabLayout: TabLayout
    private lateinit var rvAllHistory: RecyclerView
    private lateinit var rvGoalsList: RecyclerView
    private lateinit var btnBack: ImageView

    // Database & Adapter
    private lateinit var database: AppDatabase
    private lateinit var allHistoryAdapter: HistoryAdapter
    private lateinit var simpleGoalAdapter: SimpleGoalAdapter

    // Danh sách log tạm để dùng cho chức năng lọc chi tiết
    private var allLogsList: List<DepositLog> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        database = AppDatabase.getDatabase(this)

        setControl()
        setupRecyclerViews()
        setupTabs()
        loadData()
        setEvent()
    }

    private fun setControl() {
        tabLayout = findViewById(R.id.tabLayout)
        rvAllHistory = findViewById(R.id.rvAllHistory)
        rvGoalsList = findViewById(R.id.rvGoalsList)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setEvent() {
        btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerViews() {
        // 1. Adapter cho Tab "Tất cả lịch sử"
        allHistoryAdapter = HistoryAdapter()
        rvAllHistory.adapter = allHistoryAdapter
        rvAllHistory.layoutManager = LinearLayoutManager(this)

        // 2. Adapter cho Tab "Theo mục tiêu"
        simpleGoalAdapter = SimpleGoalAdapter { selectedGoal ->
            showGoalSpecificHistory(selectedGoal)
        }
        rvGoalsList.adapter = simpleGoalAdapter
        rvGoalsList.layoutManager = LinearLayoutManager(this)
    }

    private fun loadData() {
        // Lấy tất cả Logs
        lifecycleScope.launch {
            database.savingsDao().getAllLogs().collect { logs ->
                allLogsList = logs // Lưu lại để dùng lọc
                allHistoryAdapter.submitList(logs)
            }
        }

        // Lấy tất cả Goals
        lifecycleScope.launch {
            database.savingsDao().getAllGoals().collect { goals ->
                simpleGoalAdapter.submitList(goals)
            }
        }
    }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { // Tab "Tất cả"
                        rvAllHistory.visibility = View.VISIBLE
                        rvGoalsList.visibility = View.GONE
                    }
                    1 -> { // Tab "Theo mục tiêu"
                        rvAllHistory.visibility = View.GONE
                        rvGoalsList.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    // Hiển thị Dialog lịch sử riêng cho 1 mục tiêu
    private fun showGoalSpecificHistory(goal: SavingsGoal) {
        // Lọc log từ list đã lấy
        val specificLogs = allLogsList.filter { it.goalId == goal.id }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Lịch sử: ${goal.name}")

        // Tạo RecyclerView trong Dialog
        val rvSpecific = RecyclerView(this)
        rvSpecific.layoutManager = LinearLayoutManager(this)

        val dialogAdapter = HistoryAdapter()
        dialogAdapter.submitList(specificLogs)
        rvSpecific.adapter = dialogAdapter
        rvSpecific.setPadding(32, 32, 32, 32) // Padding cho đẹp

        builder.setView(rvSpecific)

        if (specificLogs.isEmpty()) {
            builder.setMessage("Chưa có giao dịch nạp tiền nào cho mục tiêu này.")
        }

        builder.setPositiveButton("Đóng", null)
        builder.show()
    }
}