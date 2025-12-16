package com.example.doan_ltdd

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.text.NumberFormat
import java.util.Locale

// Data class phụ trợ để lưu thông tin thống kê
data class CategoryStat(
    val name: String,
    val totalCurrent: Double,
    val totalTarget: Double
) {
    val progress: Int
        get() = if (totalTarget > 0) ((totalCurrent / totalTarget) * 100).toInt() else 0
}

class CategoryStatsAdapter : RecyclerView.Adapter<CategoryStatsAdapter.ViewHolder>() {

    private var list = listOf<CategoryStat>()

    fun submitList(newList: List<CategoryStat>) {
        list = newList
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvCategoryName)
        val tvPercent: TextView = view.findViewById(R.id.tvCategoryPercent)
        val progressBar: LinearProgressIndicator = view.findViewById(R.id.progressCategory)
        val tvAmount: TextView = view.findViewById(R.id.tvCategoryAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_stat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvName.text = item.name
        holder.tvPercent.text = "${item.progress}%"
        holder.progressBar.progress = item.progress

        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        val current = formatter.format(item.totalCurrent)
        val target = formatter.format(item.totalTarget)

        // Rút gọn hiển thị nếu quá dài (Tùy chọn)
        holder.tvAmount.text = "$current / $target"
    }

    override fun getItemCount() = list.size
}