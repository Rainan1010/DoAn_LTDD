package com.example.doan_ltdd

import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
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
import androidx.core.app.NotificationCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Calendar
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
    private var currentUser: User? = null
    private var currentUsername: String = ""

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
        currentUsername = intent.getStringExtra("USERNAME_KEY") ?: ""
        initializeSampleData()
        setControl()
        setEvent()
        checkAndSendNotifications()
        loadUserInfo()
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
                R.id.nav_statistics -> {
                    val intent = Intent(this, StatisticsActivity::class.java)
                    startActivity(intent)
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.nav_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
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
        val btnAddCategory = dialogView.findViewById<ImageView>(R.id.btnAddCategory)
        val spinnerPriority = dialogView.findViewById<Spinner>(R.id.spinnerPriority)
        val etDeadline = dialogView.findViewById<EditText>(R.id.etDeadline)

        // 2. Setup Priority Spinner
        val priorities = listOf("Thấp", "Trung bình", "Cao")
        spinnerPriority.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, priorities)

        // 3. Setup Category Spinner
        val categoryList = mutableListOf<String>()
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoryList)
        spinnerCategory.adapter = categoryAdapter

        lifecycleScope.launch {
            database.categoryDao().getAllCategories().collect { categories ->
                categoryList.clear()
                categoryList.addAll(categories.map { it.name })
                categoryAdapter.notifyDataSetChanged()

                if (goalToEdit != null) {
                    val index = categoryList.indexOf(goalToEdit.category)
                    if (index >= 0) spinnerCategory.setSelection(index)
                }
            }
        }

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
            selectedIconId = goalToEdit.iconResId
            ivGoalIcon.setImageResource(selectedIconId)

            val priorityIndex = priorities.indexOf(goalToEdit.priority)
            if (priorityIndex >= 0) spinnerPriority.setSelection(priorityIndex)

            selectedDate = goalToEdit.deadline
            etDeadline.setText("${selectedDate.dayOfMonth}/${selectedDate.monthValue}/${selectedDate.year}")
        } else {
            etDeadline.setText("${selectedDate.dayOfMonth}/${selectedDate.monthValue}/${selectedDate.year}")
        }

        etDeadline.setOnClickListener {
            val calendar = Calendar.getInstance()
            if (goalToEdit != null) calendar.set(goalToEdit.deadline.year, goalToEdit.deadline.monthValue - 1, goalToEdit.deadline.dayOfMonth)
            DatePickerDialog(this, { _, year, month, day ->
                selectedDate = LocalDateTime.of(year, month + 1, day, 23, 59)
                etDeadline.setText("$day/${month + 1}/$year")
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

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

        btnSave.setOnClickListener {
            val name = etGoalName.text.toString()
            val amountStr = etTargetAmount.text.toString()
            val selectedCategory = spinnerCategory.selectedItem?.toString() ?: "Khác"

            if (name.isBlank() || amountStr.isBlank()) {
                Toast.makeText(this, "Vui lòng nhập tên và số tiền!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                if (goalToEdit == null) {
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
                    // Update: Tạo bản sao để tránh lỗi reference trong Adapter
                    val updatedGoal = goalToEdit.copy(
                        name = name,
                        targetAmount = amountStr.toDouble(),
                        category = selectedCategory,
                        priority = spinnerPriority.selectedItem.toString(),
                        deadline = selectedDate,
                        iconResId = selectedIconId
                    )
                    database.savingsDao().updateGoal(updatedGoal)
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

        val categoryList = mutableListOf<String>()
        val catAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoryList)
        spinnerCategory.adapter = catAdapter

        lifecycleScope.launch {
            database.categoryDao().getAllCategories().collect { categories ->
                categoryList.clear()
                categoryList.add("Tất cả")
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

                // Tạo bản sao mới để update UI correctly
                val updatedGoal = goal.copy(currentAmount = goal.currentAmount + amount)

                val log = DepositLog(
                    goalId = updatedGoal.id,
                    goalName = updatedGoal.name,
                    amount = amount
                )

                lifecycleScope.launch {
                    database.savingsDao().updateGoal(updatedGoal)
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
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveNoti)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelNoti)

        // Đã xóa tvTimePicker ở đây

        val days = (1..31).map { "Ngày $it" }
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, days)
        spinnerDayOfMonth.adapter = spinnerAdapter

        switchNotification.isChecked = goal.isReminderEnabled

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
            // Không còn tvTimePicker để enable/disable
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

        // Đã xóa sự kiện click tvTimePicker

        btnSave.setOnClickListener {
            // Tạo bản sao mới để update
            val updatedGoal = goal.copy(
                isReminderEnabled = switchNotification.isChecked
            )

            if (updatedGoal.isReminderEnabled) {
                if (rbMonthly.isChecked) {
                    updatedGoal.reminderFrequency = "MONTHLY"
                    updatedGoal.reminderDayOfMonth = spinnerDayOfMonth.selectedItemPosition + 1
                } else {
                    updatedGoal.reminderFrequency = "DAILY"
                    // Không lưu reminderHour/Minute riêng lẻ nữa
                    // Worker sẽ tự lấy giờ từ Global Settings
                }
            }

            lifecycleScope.launch {
                database.savingsDao().updateGoal(updatedGoal)
                Toast.makeText(this@MainActivity, "Đã lưu cài đặt thông báo!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun initializeSampleData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val savingsDao = database.savingsDao()
            val categoryDao = database.categoryDao()
            val notificationDao = database.notificationDao()

            if (categoryDao.getCount() == 0) {
                val defaultCategories = SampleDataProvider.getAllCategories()
                defaultCategories.forEach { name ->
                    categoryDao.insert(Category(name))
                }
            }

            if (savingsDao.getCount() == 0) {
                val goals = SampleDataProvider.getSavingsGoals()
                val deposits = SampleDataProvider.getDepositLogs(goals)
                val notifications = SampleDataProvider.getNotificationLogs()

                savingsDao.insertAllGoals(goals)
                savingsDao.insertAllDeposits(deposits)
                notificationDao.insertAll(notifications)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Đã khởi tạo dữ liệu mẫu!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun checkAndSendNotifications() {
        lifecycleScope.launch(Dispatchers.IO) {
            val goals = database.savingsDao().getAllGoalsList()
            val now = LocalDateTime.now()
            val startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0)

            // Lấy giờ cài đặt chung
            val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            val globalHour = sharedPref.getInt("global_hour", 9)
            val globalMinute = sharedPref.getInt("global_minute", 0)

            goals.forEach { goal ->
                // 1. LOGIC NHẮC NHỞ ĐỊNH KỲ
                if (goal.isReminderEnabled) {
                    var shouldNotify = false

                    // Logic mới: "Đã quá giờ hẹn chưa?" thay vì "Có đúng giờ hẹn không?"
                    // Ví dụ: Hẹn 9h00, mở app lúc 9h30 -> Vẫn báo (nếu chưa báo)
                    // Hẹn 9h00, mở app lúc 8h30 -> Chưa báo

                    val isPastTime = (now.hour > globalHour) || (now.hour == globalHour && now.minute >= globalMinute)

                    if (goal.reminderFrequency == "DAILY") {
                        if (isPastTime) {
                            shouldNotify = true
                        }
                    } else if (goal.reminderFrequency == "MONTHLY") {
                        if (now.dayOfMonth == goal.reminderDayOfMonth && isPastTime) {
                            shouldNotify = true
                        }
                    }

                    if (shouldNotify) {
                        createNotificationIfNotExists(
                            "Nhắc nhở: ${goal.name}",
                            "Đừng quên tiết kiệm cho mục tiêu ${goal.name} nhé!",
                            startOfDay
                        )
                    }
                }

                // 2. LOGIC DEADLINE (Check lúc 9h sáng hoặc sau đó)
                if (goal.currentAmount < goal.targetAmount && now.hour >= 9) {
                    val daysLeft = ChronoUnit.DAYS.between(now.toLocalDate(), goal.deadline.toLocalDate())
                    if (daysLeft in 0..3) {
                        val title = "Sắp đến hạn: ${goal.name}"
                        val msg = "Chỉ còn $daysLeft ngày nữa là đến hạn mục tiêu ${goal.name}."
                        createNotificationIfNotExists(title, msg, startOfDay)
                    }
                }
            }
        }
    }

    // [MỚI] Hàm tạo thông báo (Kiểm tra xem hôm nay đã báo chưa)
    private suspend fun createNotificationIfNotExists(title: String, message: String, startOfDay: LocalDateTime) {
        // Kiểm tra trong DB xem hôm nay đã có thông báo tiêu đề này chưa
        val count = database.notificationDao().checkExistsToday(title, startOfDay)

        if (count == 0) {
            // A. Lưu vào DB
            val log = NotificationLog(
                title = title,
                message = message,
                timestamp = LocalDateTime.now()
            )
            database.notificationDao().insert(log)

            // B. Hiện thông báo lên thanh trạng thái
            withContext(Dispatchers.Main) {
                showSystemNotification(title, message)
            }
        }
    }

    // [MỚI] Hàm hiển thị UI thông báo (Copy từ Worker cũ sang)
    private fun showSystemNotification(title: String, message: String) {
        val channelId = "savings_reminder_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Nhắc nhở tiết kiệm", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications) // Đảm bảo bạn có icon này
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        //notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    private fun loadUserInfo() {
        if (currentUsername.isNotEmpty()) {
            lifecycleScope.launch {
                currentUser = database.userDao().getUserByUsername(currentUsername)
                currentUser?.let { user ->
                    updateNavHeader(user)
                }
            }
        }
    }
    private fun updateNavHeader(user: User) {
        val headerView = navigationView.getHeaderView(0)
        val tvUserName = headerView.findViewById<TextView>(R.id.tvUserName)
        val tvUserEmail = headerView.findViewById<TextView>(R.id.tvUserEmail)
        val imgAvatar = headerView.findViewById<ImageView>(R.id.imgAvatar)
        val btnLogout = headerView.findViewById<ImageView>(R.id.btnLogout)

        // Hiển thị tên (ưu tiên FullName, nếu không có thì dùng Username)
        tvUserName.text = if (user.fullName.isNotBlank()) user.fullName else user.username

        // Hiển thị Email (nếu chưa có thì gợi ý cập nhật)
        tvUserEmail.text = if (user.email.isNotBlank()) user.email else "Cập nhật email ngay"

        // Sự kiện Logout
        btnLogout.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Sự kiện Click Avatar -> Update Profile
        imgAvatar.setOnClickListener {
            showUpdateProfileDialog()
        }
    }

    // [MỚI] Popup Cập nhật thông tin User
    private fun showUpdateProfileDialog() {
        val user = currentUser ?: return
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_update_profile, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val etName = dialogView.findViewById<EditText>(R.id.etUpdateFullName)
        val etEmail = dialogView.findViewById<EditText>(R.id.etUpdateEmail)
        val etPass = dialogView.findViewById<EditText>(R.id.etUpdatePass)
        val btnSave = dialogView.findViewById<Button>(R.id.btnUpdateSave)

        // Fill dữ liệu hiện tại
        etName.setText(user.fullName)
        etEmail.setText(user.email)

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            val newEmail = etEmail.text.toString().trim()
            val newPass = etPass.text.toString().trim()

            // Tạo object user mới (giữ nguyên username)
            val updatedUser = user.copy(
                fullName = newName,
                email = newEmail,
                password = if (newPass.isNotEmpty()) newPass else user.password // Chỉ đổi pass nếu người dùng nhập
            )

            lifecycleScope.launch {
                database.userDao().updateUser(updatedUser)
                currentUser = updatedUser // Update biến tạm
                updateNavHeader(updatedUser) // Update UI
                Toast.makeText(this@MainActivity, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        dialog.show()
    }
}