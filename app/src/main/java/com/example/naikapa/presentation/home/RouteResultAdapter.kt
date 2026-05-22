package com.example.naikapa.presentation.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.naikapa.R
import com.example.naikapa.data.model.ScoredRoute
import com.google.android.material.card.MaterialCardView
import java.util.Locale

/**
 * RecyclerView adapter untuk menampilkan daftar rekomendasi rute (utama + alternatif).
 */
class RouteResultAdapter(
    private val context: Context,
    private var items: List<ScoredRoute> = emptyList(),
    var onItemClick: ((ScoredRoute) -> Unit)? = null
) : RecyclerView.Adapter<RouteResultAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardRankBadge: MaterialCardView = view.findViewById(R.id.cardRankBadge)
        val tvRankLabel: TextView = view.findViewById(R.id.tvRankLabel)
        val tvModeLabel: TextView = view.findViewById(R.id.tvModeLabel)
        val cardScore: MaterialCardView = view.findViewById(R.id.cardScore)
        val tvScore: TextView = view.findViewById(R.id.tvScore)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvCost: TextView = view.findViewById(R.id.tvCost)
        val tvWalking: TextView = view.findViewById(R.id.tvWalking)
        val tvTransit: TextView = view.findViewById(R.id.tvTransit)
        val tvReason: TextView = view.findViewById(R.id.tvReason)
        val tvDisruptionWarning: TextView = view.findViewById(R.id.tvDisruptionWarning)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route_recommendation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val m = item.candidate.metrics
        val isMain = position == 0

        // Rank badge
        holder.tvRankLabel.text = item.rankLabel.uppercase(Locale.getDefault())
        val rankBgColor = if (isMain) R.color.colorAccentOrangeLight else R.color.colorBackground
        val rankTextColor = if (isMain) R.color.colorAccentOrange else R.color.colorTextSecondary
        holder.cardRankBadge.setCardBackgroundColor(ContextCompat.getColor(context, rankBgColor))
        holder.tvRankLabel.setTextColor(ContextCompat.getColor(context, rankTextColor))

        // Moda label
        holder.tvModeLabel.text = item.candidate.candidateLabel

        // Skor — warna berdasarkan rentang
        holder.tvScore.text = item.score.toString()
        val scoreColor = when {
            item.score >= 85 -> R.color.colorPrimary
            item.score >= 70 -> R.color.colorSecondary
            item.score >= 55 -> R.color.colorAccentOrange
            else             -> R.color.colorError
        }
        holder.tvScore.setTextColor(ContextCompat.getColor(context, scoreColor))
        // cardScore sekarang hidden, tidak perlu di-update background-nya

        // Metrics
        holder.tvTime.text = formatDuration(m.totalDurationSeconds)
        holder.tvCost.text = formatRupiah(m.estimatedTotalCost)
        holder.tvWalking.text = formatDistance(m.walkingDistanceMeters)
        holder.tvTransit.text = "${m.transitCount}x"

        // Alasan
        holder.tvReason.text = item.reason

        // Warning gangguan
        if (item.hasDisruptionWarning && !item.disruptionWarningText.isNullOrBlank()) {
            holder.tvDisruptionWarning.text = item.disruptionWarningText
            holder.tvDisruptionWarning.visibility = View.VISIBLE
        } else {
            holder.tvDisruptionWarning.visibility = View.GONE
        }

        // Click listener
        holder.itemView.setOnClickListener { onItemClick?.invoke(item) }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<ScoredRoute>) {
        items = newItems
        notifyDataSetChanged()
    }

    // ── Format helpers ────────────────────────────────────────────────────────

    private fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            hours > 0 -> "${hours} j ${minutes} mnt"
            else      -> "${minutes} mnt"
        }
    }

    private fun formatRupiah(amount: Int): String {
        if (amount == 0) return "Gratis"
        return "Rp ${String.format("%,d", amount).replace(',', '.')}"
    }

    private fun formatDistance(meters: Double): String {
        return when {
            meters == 0.0 -> "0 m"
            meters < 1000 -> "${meters.toInt()} m"
            else          -> String.format("%.1f km", meters / 1000.0)
        }
    }
}
