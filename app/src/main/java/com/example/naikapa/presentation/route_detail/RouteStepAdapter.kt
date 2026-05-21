package com.example.naikapa.presentation.route_detail

import android.graphics.Color
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.naikapa.R

data class DetailStep(
    val title: String,
    val description: String,
    val durationSeconds: Int,
    val distanceMeters: Double,
    val iconResId: Int,
    val colorInt: Int
)

class RouteStepAdapter(private val steps: List<DetailStep>) :
    RecyclerView.Adapter<RouteStepAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val viewLineTop: View = view.findViewById(R.id.viewLineTop)
        val viewLineBottom: View = view.findViewById(R.id.viewLineBottom)
        val cardDot: CardView = view.findViewById(R.id.cardDot)
        val cardIcon: CardView = view.findViewById(R.id.cardIcon)
        val ivStepIcon: ImageView = view.findViewById(R.id.ivStepIcon)
        val tvStepTitle: TextView = view.findViewById(R.id.tvStepTitle)
        val tvStepDescription: TextView = view.findViewById(R.id.tvStepDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route_step, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val step = steps[position]

        // Handle line visibility
        holder.viewLineTop.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
        holder.viewLineBottom.visibility = if (position == steps.size - 1) View.INVISIBLE else View.VISIBLE

        // Parse color
        val colorStateList = ColorStateList.valueOf(step.colorInt)

        // Bind dot color
        holder.cardDot.setCardBackgroundColor(colorStateList)

        // Bind icon and tint
        holder.ivStepIcon.setImageResource(step.iconResId)
        holder.ivStepIcon.imageTintList = colorStateList

        // Bind texts
        holder.tvStepTitle.text = step.title

        val distanceText = formatDistance(step.distanceMeters)
        val durationText = formatDuration(step.durationSeconds)
        holder.tvStepDescription.text = if (step.distanceMeters > 0) {
            "$distanceText • $durationText"
        } else {
            durationText
        }
    }

    override fun getItemCount(): Int = steps.size

    private fun formatDuration(seconds: Int): String {
        val minutes = (seconds + 59) / 60
        return if (minutes >= 60) {
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            if (remainingMinutes > 0) {
                "$hours jam $remainingMinutes mnt"
            } else {
                "$hours jam"
            }
        } else {
            "$minutes mnt"
        }
    }

    private fun formatDistance(meters: Double): String {
        return if (meters >= 1000) {
            val km = meters / 1000.0
            String.format("%.1f km", km)
        } else {
            "${meters.toInt()} m"
        }
    }
}
