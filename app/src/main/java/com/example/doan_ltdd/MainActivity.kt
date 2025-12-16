package com.example.doan_ltdd

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var goalAdapter: GoalAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var rvGoals: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var btnMenu: ImageView
    private lateinit var navigationView: NavigationView
    private lateinit var btnNotification: ImageView
    private lateinit var btnSearch: ImageView

    // Biến lưu danh sách gốc để phục vụ chức năng tìm kiếm/lọc
    private var originalGoals: List<SavingsGoal> = listOf()

    private val availableIcons = listOf(
        R.drawable.ic_beach,
        R.drawable.ic_car,
        R.drawable.ic_home,
        R.drawable.ic_health,
        R.drawable.ic_heart_smile,
        R.drawable.ic_person_heart,
        R.drawable.ic_mobile,
        R.drawable.ic_motorcycle,
        R.drawable.ic_pets,
        R.drawable.ic_partne,
        R.drawable.ic_travel
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = AppDatabase.getDatabase(this)

        initializeSampleData()
        setControl()
        setEvent()
    }

    private fun setControl() {
        drawerLayout = findViewById(R.id.drawerLayout)
        rvGoals = findViewById(R.id.rvGoals)
        fabAdd = findViewById(R.id.fabAdd)
        btnMenu = findViewById(R.id.btnMenu)
        navigationView = findViewById(R.id.navigationView)
        btnNotification = findViewById(R.id.btnNotification)
        btnSearch = findViewById(R.id.btnSearch)

        goalAdapter = GoalAdapter(
            onDepositClick = { goal -> showDepositDialog(goal) },
            onMenuClick = { goal, view -> showGoalOptionMenu(goal, view) }
        )

        rvGoals.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = goalAdapter
        }
    }

    private fun setEvent() {
        // Load danh sách Goals và lưu vào originalGoals
        lifecycleScope.launch {
            database.savingsDao().getAllGoals().collect { goals ->
                originalGoals = goals
                goalAdapter.submitList(goals)
            }
        }

        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        fabAdd.setOnClickListener {
            showAddGoalDialog()
        }

        btnSearch.setOnClickListener {
            showSearchDialog()
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> drawerLayout.closeDrawer(GravityCompat.START)
                R.id.nav_history -> {
                    val intent = Intent(this, HistoryActivity::class.java)
                    startActivity(intent)
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
            }
            true
        }

        btnNotification.setOnClickListener {
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)
        }

        setupWorker()
    }

    // --- DIALOG THÊM / SỬA MỤC TIÊU ---
    private fun showAddGoalDialog(goalToEdit: SavingsGoal? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_goal, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        // 1. Ánh xạ View
        val ivGoalIcon = dialogView.findViewById<ImageView>(R.id.ivGoalIcon)
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etGoalName = dialogView.findViewById<EditText>(R.id.etGoalName)
        val etTargetAmount = dialogView.findViewById<EditText>(R.id.etTargetAmount)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val btnAddCategory = dialogView.findViewById<ImageView>(R.id.btnAddCategory) // Nút thêm Category mới
        val spinnerPriority = dialogView.findViewById<Spinner>(R.id.spinnerPriority)
        val etDeadline = dialogView.findViewById<EditText>(R.id.etDeadline)

        // 2. Setup Priority Spinner (Cố định)
        val priorities = listOf("Thấp", "Trung bình", "Cao")
        spinnerPriority.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, priorities)

        // 3. Setup Category Spinner (Động từ DB)
        val categoryList = mutableListOf<String>()
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoryList)
        spinnerCategory.adapter = categoryAdapter

        // Lấy danh sách category từ DB
        lifecycleScope.launch {
            database.categoryDao().getAllCategories().collect { categories ->
                categoryList.clear()
                categoryList.addAll(categories.map { it.name })
                categoryAdapter.notifyDataSetChanged()

                // Nếu đang Edit, set lại selection
                if (goalToEdit != null) {
                    val index = categoryList.indexOf(goalToEdit.category)
                    if (index >= 0) spinnerCategory.setSelection(index)
                }
            }
        }

        // Xử lý nút thêm category mới
        btnAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }

        // --- BIẾN LƯU TRẠNG THÁI ---
        var selectedDate: LocalDateTime = LocalDateTime.now().plusMonths(1)
        var selectedIconId: Int = availableIcons[0]

        // --- KIỂM TRA CHẾ ĐỘ SỬA ---
        if (goalToEdit != null) {
            tvDialogTitle.text = "Chỉnh Sửa Mục Tiêu"
            etGoalName.setText(goalToEdit.name)
            etTargetAmount.setText(String.format("%.0f", goalToEdit.targetAmount))

            // Load icon cũ
            selectedIconId = goalToEdit.iconResId
            ivGoalIcon.setImageResource(selectedIconId)

            val priorityIndex = priorities.indexOf(goalToEdit.priority)
            if (priorityIndex >= 0) spinnerPriority.setSelection(priorityIndex)

            selectedDate = goalToEdit.deadline
            etDeadline.setText("${selectedDate.dayOfMonth}/${selectedDate.monthValue}/${selectedDate.year}")
        } else {
            // Mặc định hiển thị ngày
            etDeadline.setText("${selectedDate.dayOfMonth}/${selectedDate.monthValue}/${selectedDate.year}")
        }

        // Xử lý chọn ngày
        etDeadline.setOnClickListener {
            val calendar = Calendar.getInstance()
            if (goalToEdit != null) calendar.set(goalToEdit.deadline.year, goalToEdit.deadline.monthValue - 1, goalToEdit.deadline.dayOfMonth)
            DatePickerDialog(this, { _, year, month, day ->
                selectedDate = LocalDateTime.of(year, month + 1, day, 23, 59)
                etDeadline.setText("$day/${month + 1}/$year")
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Xử lý chọn Icon
        ivGoalIcon.setOnClickListener {
            val iconAdapter = object : ArrayAdapter<Int>(this, android.R.layout.select_dialog_item, availableIcons) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent) as TextView
                    view.text = ""
                    view.setCompoundDrawablesWithIntrinsicBounds(availableIcons[position], 0, 0, 0)
                    view.compoundDrawablePadding = 24
                    return view
                }
            }

            AlertDialog.Builder(this)
                .setTitle("Chọn biểu tượng")
                .setAdapter(iconAdapter) { dialogInterface, which ->
                    selectedIconId = availableIcons[which]
                    ivGoalIcon.setImageResource(selectedIconId)
                    dialogInterface.dismiss()
                }
                .create()
                .show()
        }

        // Xử lý nút LƯU
        btnSave.setOnClickListener {
            val name = etGoalName.text.toString()
            val amountStr = etTargetAmount.text.toString()
            val selectedCategory = spinnerCategory.selectedItem?.toString() ?: "Khác" // Phòng trường hợp chưa có category nào

            if (name.isBlank() || amountStr.isBlank()) {
                Toast.makeText(this, "Vui lòng nhập tên và số tiền!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                if (goalToEdit == null) {
                    // --- THÊM MỚI ---
                    val newGoal = SavingsGoal(
                        name = name,
                        targetAmount = amountStr.toDouble(),
                        category = selectedCategory,
                        priority = spinnerPriority.selectedItem.toString(),
                        deadline = selectedDate,
                        iconResId = selectedIconId
                    )
                    database.savingsDao().insertGoal(newGoal)
                    Toast.makeText(this@MainActivity, "Đã thêm mục tiêu!", Toast.LENGTH_SHORT).show()
                } else {
                    // --- CẬP NHẬT ---
                    goalToEdit.name = name
                    goalToEdit.targetAmount = amountStr.toDouble()
                    goalToEdit.category = selectedCategory
                    goalToEdit.priority = spinnerPriority.selectedItem.toString()
                    goalToEdit.deadline = selectedDate
                    goalToEdit.iconResId = selectedIconId

                    database.savingsDao().updateGoal(goalToEdit)
                    Toast.makeText(this@MainActivity, "Đã cập nhật thành công!", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    // --- DIALOG THÊM DANH MỤC MỚI ---
    private fun showAddCategoryDialog() {
        val input = EditText(this)
        input.hint = "Nhập tên danh mục mới"
        input.setPadding(50, 30, 50, 30)

        AlertDialog.Builder(this)
            .setTitle("Thêm Danh Mục")
            .setView(input)
            .setPositiveButton("Thêm") { _, _ ->
                val newCategoryName = input.text.toString().trim()
                if (newCategoryName.isNotEmpty()) {
                    lifecycleScope.launch {
                        database.categoryDao().insert(Category(newCategoryName))
                        Toast.makeText(this@MainActivity, "Đã thêm danh mục: $newCategoryName", Toast.LENGTH_SHORT).show()
                        // Flow sẽ tự update spinner ở dialog cha
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // --- DIALOG TÌM KIẾM ---
    private fun showSearchDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_search, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val etSearchName = dialogView.findViewById<EditText>(R.id.etSearchName)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerSearchCategory)
        val spinnerPriority = dialogView.findViewById<Spinner>(R.id.spinnerSearchPriority)
        val btnSearchAction = dialogView.findViewById<Button>(R.id.btnPerformSearch)
        val btnReset = dialogView.findViewById<Button>(R.id.btnResetSearch)

        // Setup Category Spinner cho Search (Thêm "Tất cả")
        val categoryList = mutableListOf<String>()
        val catAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoryList)
        spinnerCategory.adapter = catAdapter

        // Lấy danh sách category hiện có
        lifecycleScope.launch {
            database.categoryDao().getAllCategories().collect { categories ->
                categoryList.clear()
                categoryList.add("Tất cả") // Luôn có tùy chọn này đầu tiên
                categoryList.addAll(categories.map { it.name })
                catAdapter.notifyDataSetChanged()
            }
        }

        val priorities = listOf("Tất cả", "Thấp", "Trung bình", "Cao")
        spinnerPriority.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, priorities)

        btnSearchAction.setOnClickListener {
            val keyword = etSearchName.text.toString().trim().lowercase()
            val selectedCategory = spinnerCategory.selectedItem.toString()
            val selectedPriority = spinnerPriority.selectedItem.toString()

            val filteredList = originalGoals.filter { goal ->
                val matchName = keyword.isEmpty() || goal.name.lowercase().contains(keyword)
                val matchCategory = selectedCategory == "Tất cả" || goal.category == selectedCategory
                val matchPriority = selectedPriority == "Tất cả" || goal.priority == selectedPriority
                matchName && matchCategory && matchPriority
            }

            if (filteredList.isEmpty()) {
                Toast.makeText(this, "Không tìm thấy kết quả nào!", Toast.LENGTH_SHORT).show()
            }
            goalAdapter.submitList(filteredList)
            dialog.dismiss()
        }

        btnReset.setOnClickListener {
            goalAdapter.submitList(originalGoals)
            dialog.dismiss()
            Toast.makeText(this, "Đã hiển thị lại tất cả mục tiêu", Toast.LENGTH_SHORT).show()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    // --- DIALOG NẠP TIỀN ---
    private fun showDepositDialog(goal: SavingsGoal) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_deposit, null)

        val tvCurrentGoalName = dialogView.findViewById<TextView>(R.id.tvCurrentGoalName)
        val etDepositAmount = dialogView.findViewById<EditText>(R.id.etDepositAmount)
        val btnConfirmDeposit = dialogView.findViewById<Button>(R.id.btnConfirmDeposit)
        val btnCancelDeposit = dialogView.findViewById<Button>(R.id.btnCancelDeposit)

        tvCurrentGoalName.text = "Cho mục tiêu: ${goal.name}"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnConfirmDeposit.setOnClickListener {
            val amountStr = etDepositAmount.text.toString()
            if (amountStr.isNotBlank()) {
                val amount = amountStr.toDouble()
                goal.addDeposit(amount)
                val log = DepositLog(goalId = goal.id, goalName = goal.name, amount = amount)

                lifecycleScope.launch {
                    database.savingsDao().updateGoal(goal)
                    database.savingsDao().insertLog(log)
                    dialog.dismiss()
                    Toast.makeText(this@MainActivity, "Nạp tiền thành công!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnCancelDeposit.setOnClickListener { dialog.dismiss() }
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    // --- MENU OPTIONS (Sửa/Xóa/Notify) ---
    private fun showGoalOptionMenu(goal: SavingsGoal, view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.menu_goal_options, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_notification -> {
                    showNotificationDialog(goal)
                    true
                }
                R.id.action_edit -> {
                    showAddGoalDialog(goal)
                    true
                }
                R.id.action_delete -> {
                    AlertDialog.Builder(this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc muốn xóa mục tiêu '${goal.name}'?")
                        .setPositiveButton("Xóa") { _, _ ->
                            lifecycleScope.launch {
                                database.savingsDao().deleteGoal(goal)
                            }
                        }
                        .setNegativeButton("Hủy", null)
                        .show()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    // --- DIALOG CÀI ĐẶT THÔNG BÁO ---
    private fun showNotificationDialog(goal: SavingsGoal) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_notification, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val switchNotification = dialogView.findViewById<SwitchCompat>(R.id.switchNotification)
        val layoutOptions = dialogView.findViewById<LinearLayout>(R.id.layoutOptions)
        val rbMonthly = dialogView.findViewById<RadioButton>(R.id.rbMonthly)
        val rbDaily = dialogView.findViewById<RadioButton>(R.id.rbDaily)
        val spinnerDayOfMonth = dialogView.findViewById<Spinner>(R.id.spinnerDayOfMonth)
        val tvTimePicker = dialogView.findViewById<TextView>(R.id.tvTimePicker)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveNoti)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelNoti)

        val days = (1..31).map { "Ngày $it" }
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, days)
        spinnerDayOfMonth.adapter = spinnerAdapter

        switchNotification.isChecked = goal.isReminderEnabled
        var tempHour = goal.reminderHour
        var tempMinute = goal.reminderMinute
        tvTimePicker.text = String.format("%02d:%02d", tempHour, tempMinute)

        if (goal.reminderFrequency == "MONTHLY") {
            rbMonthly.isChecked = true
            rbDaily.isChecked = false
        } else {
            rbMonthly.isChecked = false
            rbDaily.isChecked = true
        }

        if (goal.reminderDayOfMonth in 1..31) {
            spinnerDayOfMonth.setSelection(goal.reminderDayOfMonth - 1)
        }

        fun updateUIState(isEnabled: Boolean) {
            layoutOptions.alpha = if (isEnabled) 1.0f else 0.5f
            rbMonthly.isEnabled = isEnabled
            rbDaily.isEnabled = isEnabled
            spinnerDayOfMonth.isEnabled = isEnabled && rbMonthly.isChecked
            tvTimePicker.isEnabled = isEnabled && rbDaily.isChecked
        }
        updateUIState(switchNotification.isChecked)

        switchNotification.setOnCheckedChangeListener { _, isChecked -> updateUIState(isChecked) }

        rbMonthly.setOnClickListener {
            rbDaily.isChecked = false
            updateUIState(true)
        }
        rbDaily.setOnClickListener {
            rbMonthly.isChecked = false
            updateUIState(true)
        }

        tvTimePicker.setOnClickListener {
            TimePickerDialog(this, { _, hourOfDay, minute ->
                tempHour = hourOfDay
                tempMinute = minute
                tvTimePicker.text = String.format("%02d:%02d", tempHour, tempMinute)
            }, tempHour, tempMinute, true).show()
        }

        btnSave.setOnClickListener {
            goal.isReminderEnabled = switchNotification.isChecked
            if (goal.isReminderEnabled) {
                if (rbMonthly.isChecked) {
                    goal.reminderFrequency = "MONTHLY"
                    goal.reminderDayOfMonth = spinnerDayOfMonth.selectedItemPosition + 1
                } else {
                    goal.reminderFrequency = "DAILY"
                    goal.reminderHour = tempHour
                    goal.reminderMinute = tempMinute
                }
            }
            lifecycleScope.launch {
                database.savingsDao().updateGoal(goal)
                Toast.makeText(this@MainActivity, "Đã lưu cài đặt thông báo!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun setupWorker() {
        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun initializeSampleData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val savingsDao = database.savingsDao()
            val categoryDao = database.categoryDao()
            val notificationDao = database.notificationDao() // Đừng quên DAO này

            // 1. Khởi tạo danh mục mẫu (Lấy từ SampleDataProvider để đồng bộ)
            if (categoryDao.getCount() == 0) {
                // SỬ DỤNG HÀM MỚI TRONG SampleDataProvider
                val defaultCategories = SampleDataProvider.getAllCategories()
                defaultCategories.forEach { name ->
                    categoryDao.insert(Category(name))
                }
                Log.d("DB_INIT", "Đã khởi tạo danh mục mẫu")
            }

            // 2. Khởi tạo Goals, Logs, Notifications mẫu
            if (savingsDao.getCount() == 0) {
                Log.d("DB_INIT", "Database trống, bắt đầu thêm dữ liệu mẫu...")

                val goals = SampleDataProvider.getSavingsGoals()
                val deposits = SampleDataProvider.getDepositLogs(goals)
                val notifications = SampleDataProvider.getNotificationLogs()

                savingsDao.insertAllGoals(goals)
                savingsDao.insertAllDeposits(deposits)
                notificationDao.insertAll(notifications)

                Log.d("DB_INIT", "Đã thêm dữ liệu mẫu thành công!")

                // Cập nhật lại UI trên Main Thread
                withContext(Dispatchers.Main) {
                    // Refresh lại list nếu cần (thường Flow sẽ tự lo việc này)
                    Toast.makeText(this@MainActivity, "Đã khởi tạo dữ liệu mẫu!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}