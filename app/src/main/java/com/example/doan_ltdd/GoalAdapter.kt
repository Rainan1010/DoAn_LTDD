package com.example.doan_ltdd

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.text.NumberFormat
import java.time.temporal.ChronoUnit
import java.util.Locale

class GoalAdapter(
    private val onDepositClick: (SavingsGoal) -> Unit,
    private val onMenuClick: (SavingsGoal, View) -> Unit
) : ListAdapter<SavingsGoal, GoalAdapter.GoalViewHolder>(GoalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_goal, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // --- 1. Ánh xạ View (Gộp tất cả vào đây) ---
        private val ivGoalIconItem: ImageView = itemView.findViewById(R.id.ivGoalIconItem) // Đã chuyển lên đây
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val progressBar: LinearProgressIndicator = itemView.findViewById(R.id.progressBar)
        private val tvPercent: TextView = itemView.findViewById(R.id.tvPercent)
        private val tvPriority: Chip = itemView.findViewById(R.id.tvPriority)
        private val layoutWarning: LinearLayout = itemView.findViewById(R.id.layoutWarning)
        private val tvTimeRemaining: TextView = itemView.findViewById(R.id.tvTimeRemaining)
        private val btnMore: ImageView = itemView.findViewById(R.id.btnMore)

        fun bind(goal: SavingsGoal) {
            // --- 2. Gán dữ liệu (Gộp logic hiển thị icon vào đây) ---

            // Hiển thị Icon
            ivGoalIconItem.setImageResource(goal.iconResId)

            // Hiển thị Tên
            tvTitle.text = goal.name

            // Hiển thị Tiền
            val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            val current = formatter.format(goal.currentAmount)
            val target = formatter.format(goal.targetAmount)
            tvAmount.text = "$current / $target"

            // Hiển thị Progress
            progressBar.progress = goal.progressPercentage
            tvPercent.text = "${goal.progressPercentage}%"

            // Hiển thị Mức độ ưu tiên (Màu sắc)
            tvPriority.text = goal.priority
            val (bgColor, textColor) = when (goal.priority) {
                "Cao" -> Pair(0xFFFFEBEE.toInt(), 0xFFD32F2F.toInt())
                "Trung bình" -> Pair(0xFFE3F2FD.toInt(), 0xFF1976D2.toInt())
                else -> Pair(0xFFF5F5F5.toInt(), 0xFF616161.toInt())
            }
            tvPriority.chipBackgroundColor = ColorStateList.valueOf(bgColor)
            tvPriority.setTextColor(textColor)

            // Hiển thị Cảnh báo Deadline
            if (goal.isDeadlineApproaching(3) && goal.currentAmount < goal.targetAmount) {
                layoutWarning.visibility = View.VISIBLE
                val daysLeft = ChronoUnit.DAYS.between(java.time.LocalDateTime.now(), goal.deadline)
                tvTimeRemaining.text = if (daysLeft < 0) "Đã quá hạn" else "$daysLeft ngày nữa"
            } else {
                layoutWarning.visibility = View.GONE
            }

            // Sự kiện Click
            itemView.setOnClickListener { onDepositClick(goal) }
            btnMore.setOnClickListener { onMenuClick(goal, btnMore) }
        }
    }

    class GoalDiffCallback : DiffUtil.ItemCallback<SavingsGoal>() {
        override fun areItemsTheSame(oldItem: SavingsGoal, newItem: SavingsGoal) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SavingsGoal, newItem: SavingsGoal) = oldItem == newItem
    }
}