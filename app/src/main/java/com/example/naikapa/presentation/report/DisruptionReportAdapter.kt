package com.example.naikapa.presentation.report

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.naikapa.R
import com.example.naikapa.data.model.DisruptionReport
import java.io.File

class DisruptionReportAdapter(
    private val currentUserId: Long,
    private val onEditClick: (DisruptionReport) -> Unit,
    private val onDeleteClick: (DisruptionReport) -> Unit
) : ListAdapter<DisruptionReport, DisruptionReportAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategory: TextView     = itemView.findViewById(R.id.tvReportCategory)
        val tvLocation: TextView     = itemView.findViewById(R.id.tvReportLocation)
        val tvDescription: TextView  = itemView.findViewById(R.id.tvReportDescription)
        val tvTimeRemaining: TextView = itemView.findViewById(R.id.tvReportTimeRemaining)
        val tvStatusBadge: TextView  = itemView.findViewById(R.id.tvReportStatusBadge)
        val imgPhoto: ImageView      = itemView.findViewById(R.id.imgReportPhoto)
        val btnEdit: ImageButton     = itemView.findViewById(R.id.btnReportEdit)
        val btnDelete: ImageButton   = itemView.findViewById(R.id.btnReportDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_disruption_report, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val report = getItem(position)
        val context = holder.itemView.context
        val now = System.currentTimeMillis()

        holder.tvCategory.text = report.category
        holder.tvDescription.text = report.description

        // Lokasi: tampilkan stopId atau routeId jika ada
        val locationText = when {
            !report.stopId.isNullOrBlank() && !report.routeId.isNullOrBlank() ->
                "${report.stopId} · ${report.routeId}"
            !report.stopId.isNullOrBlank() -> report.stopId
            !report.routeId.isNullOrBlank() -> report.routeId
            else -> context.getString(R.string.report_location_unknown)
        }
        holder.tvLocation.text = locationText

        // Sisa waktu aktif
        holder.tvTimeRemaining.text = if (report.isActive(now)) {
            context.getString(R.string.report_time_remaining_format, report.remainingTimeLabel(now))
        } else {
            context.getString(R.string.report_expired_label)
        }

        // Badge status
        if (report.isActive(now)) {
            holder.tvStatusBadge.text = context.getString(R.string.report_status_active)
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_active)
            holder.tvStatusBadge.setTextColor(
                androidx.core.content.ContextCompat.getColor(context, R.color.colorSecondary)
            )
        } else {
            holder.tvStatusBadge.text = context.getString(R.string.report_status_expired)
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_expired)
            holder.tvStatusBadge.setTextColor(
                androidx.core.content.ContextCompat.getColor(context, R.color.colorTextHint)
            )
        }

        // Foto opsional
        if (!report.photoPath.isNullOrBlank()) {
            val photoFile = File(report.photoPath)
            if (photoFile.exists()) {
                holder.imgPhoto.visibility = View.VISIBLE
                holder.imgPhoto.setImageURI(android.net.Uri.fromFile(photoFile))
            } else {
                holder.imgPhoto.visibility = View.GONE
            }
        } else {
            holder.imgPhoto.visibility = View.GONE
        }

        // Tombol edit/hapus hanya untuk laporan milik user yang sedang login
        val isOwner = report.idUser == currentUserId
        holder.btnEdit.visibility   = if (isOwner) View.VISIBLE else View.GONE
        holder.btnDelete.visibility = if (isOwner) View.VISIBLE else View.GONE

        holder.btnEdit.setOnClickListener   { onEditClick(report) }
        holder.btnDelete.setOnClickListener { onDeleteClick(report) }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DisruptionReport>() {
            override fun areItemsTheSame(old: DisruptionReport, new: DisruptionReport) =
                old.idReport == new.idReport

            override fun areContentsTheSame(old: DisruptionReport, new: DisruptionReport) =
                old == new
        }
    }
}
