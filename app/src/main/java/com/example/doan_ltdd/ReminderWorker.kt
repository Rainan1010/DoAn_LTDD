package com.example.doan_ltdd

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val goals = database.savingsDao().getAllGoalsList()
        val now = LocalDateTime.now()

        // Lấy thời điểm bắt đầu ngày hôm nay (00:00:00) để check trùng lặp
        val startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0)

        goals.forEach { goal ->
            // --- LOGIC 1: NHẮC NHỞ NGƯỜI DÙNG TỰ CÀI ĐẶT (Code cũ giữ nguyên) ---
            if (goal.isReminderEnabled) {
                var shouldNotifyUserSetting = false
                if (goal.reminderFrequency == "DAILY") {
                    if (now.hour == goal.reminderHour && now.minute == goal.reminderMinute) {
                        shouldNotifyUserSetting = true
                    }
                } else if (goal.reminderFrequency == "MONTHLY") {
                    if (now.dayOfMonth == goal.reminderDayOfMonth && now.hour == 9 && now.minute == 0) {
                        shouldNotifyUserSetting = true
                    }
                }

                if (shouldNotifyUserSetting) {
                    createNotification(database, "Nhắc nhở: ${goal.name}", "Đừng quên tiết kiệm cho mục tiêu ${goal.name} nhé!", startOfDay)
                }
            }

            // --- LOGIC 2: [MỚI] TỰ ĐỘNG NHẮC DEADLINE LÚC 9H SÁNG ---
            // Kiểm tra: Chưa hoàn thành + Trong khung giờ 9h + Deadline còn <= 3 ngày
            if (goal.currentAmount < goal.targetAmount && now.hour == 9) {

                // Tính số ngày còn lại
                val daysLeft = ChronoUnit.DAYS.between(now.toLocalDate(), goal.deadline.toLocalDate())

                // Logic: Còn từ 0 đến 3 ngày (không tính số âm là đã quá hạn)
                if (daysLeft in 0..3) {
                    val title = "Sắp đến hạn: ${goal.name}"
                    val message = if (daysLeft == 0L)
                        "Hôm nay là hạn chót của mục tiêu ${goal.name}. Hãy kiểm tra ngay!"
                    else
                        "Chỉ còn $daysLeft ngày nữa là đến hạn mục tiêu ${goal.name}."

                    // Gọi hàm tạo thông báo (đã bao gồm check trùng lặp)
                    createNotification(database, title, message, startOfDay)
                }
            }
        }
        return Result.success()
    }

    // Hàm tách riêng để tái sử dụng và kiểm tra trùng lặp
    private suspend fun createNotification(
        db: AppDatabase,
        title: String,
        message: String,
        startOfDay: LocalDateTime
    ) {
        // Kiểm tra xem hôm nay đã gửi thông báo này chưa (tránh spam vì Worker chạy 15p/lần)
        val count = db.notificationDao().checkExistsToday(title, startOfDay)

        if (count == 0) {
            // 1. Lưu vào Database (để hiện trong NotificationActivity)
            val log = NotificationLog(
                title = title,
                message = message,
                timestamp = LocalDateTime.now()
            )
            db.notificationDao().insert(log)

            // 2. Bắn thông báo hệ thống
            sendSystemNotification(applicationContext, log.title, log.message)
        }
    }

    private fun sendSystemNotification(context: Context, title: String, message: String) {
        val channelId = "savings_reminder_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Nhắc nhở tiết kiệm", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        //notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}