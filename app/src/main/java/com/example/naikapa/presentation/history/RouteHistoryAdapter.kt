package com.example.naikapa.presentation.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.naikapa.R
import com.example.naikapa.data.model.RouteHistory
import java.util.Locale

class RouteHistoryAdapter(
    private val onDelete: (RouteHistory) -> Unit,
    private val onItemClick: (RouteHistory) -> Unit = {}
) : ListAdapter<RouteHistory, RouteHistoryAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<RouteHistory>() {
            override fun areItemsTheSame(a: RouteHistory, b: RouteHistory) = a.idHistory == b.idHistory
            override fun areContentsTheSame(a: RouteHistory, b: RouteHistory) = a == b
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvOrigin: TextView = view.findViewById(R.id.tvRouteHistoryOrigin)
        val tvDestination: TextView = view.findViewById(R.id.tvRouteHistoryDestination)
        val tvScore: TextView = view.findViewById(R.id.tvRouteHistoryScore)
        val tvSummary: TextView = view.findViewById(R.id.tvRouteHistorySummary)
        val tvTime: TextView = view.findViewById(R.id.tvRouteHistoryTime)
        val tvCost: TextView = view.findViewById(R.id.tvRouteHistoryCost)
        val tvTransit: TextView = view.findViewById(R.id.tvRouteHistoryTransit)
        val btnDelete: ImageView = view.findViewById(R.id.btnDeleteRouteHistory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.tvOrigin.text = item.originName
        holder.tvDestination.text = "→ ${item.destinationName}"
        holder.tvScore.text = "${item.score}% Cocok"
        holder.tvSummary.text = "${item.mode} · ${item.priority} · ${item.recommendationSummary}"
        holder.tvTime.text = formatDuration(item.estimatedTime)
        holder.tvCost.text = formatRupiah(item.estimatedCost)
        holder.tvTransit.text = "${item.transitCount}x"
        holder.btnDelete.setOnClickListener { onDelete(item) }
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    private fun formatDuration(seconds: Int): String {
        val minutes = (seconds + 59) / 60
        return if (minutes >= 60) {
            val h = minutes / 60
            val m = minutes % 60
            if (m > 0) "$h j $m mnt" else "$h j"
        } else {
            "$minutes mnt"
        }
    }

    private fun formatRupiah(value: Int): String {
        if (value == 0) return "Gratis"
        return "Rp " + String.format(Locale.US, "%,d", value).replace(",", ".")
    }
}
