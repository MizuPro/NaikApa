package com.example.naikapa.presentation.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.naikapa.R
import com.example.naikapa.data.model.SavedTrip
import com.google.android.material.button.MaterialButton

class SavedTripAdapter(
    private val onReplay: (SavedTrip) -> Unit,
    private val onEdit: (SavedTrip) -> Unit,
    private val onDelete: (SavedTrip) -> Unit
) : ListAdapter<SavedTrip, SavedTripAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SavedTrip>() {
            override fun areItemsTheSame(a: SavedTrip, b: SavedTrip) = a.idSaved == b.idSaved
            override fun areContentsTheSame(a: SavedTrip, b: SavedTrip) = a == b
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTripName: TextView = view.findViewById(R.id.tvTripName)
        val tvTripMode: TextView = view.findViewById(R.id.tvTripMode)
        val tvOriginName: TextView = view.findViewById(R.id.tvOriginName)
        val tvDestinationName: TextView = view.findViewById(R.id.tvDestinationName)
        val tvCatatan: TextView = view.findViewById(R.id.tvCatatan)
        val btnReplay: MaterialButton = view.findViewById(R.id.btnReplay)
        val btnEdit: MaterialButton = view.findViewById(R.id.btnEdit)
        val btnDelete: MaterialButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_trip, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val trip = getItem(position)
        holder.tvTripName.text = trip.namaPerjalanan
        holder.tvTripMode.text = "${trip.mode} · ${trip.priority}"
        holder.tvOriginName.text = trip.originName
        holder.tvDestinationName.text = trip.destinationName

        if (!trip.catatan.isNullOrBlank()) {
            holder.tvCatatan.text = trip.catatan
            holder.tvCatatan.visibility = View.VISIBLE
        } else {
            holder.tvCatatan.visibility = View.GONE
        }

        holder.btnReplay.setOnClickListener { onReplay(trip) }
        holder.btnEdit.setOnClickListener { onEdit(trip) }
        holder.btnDelete.setOnClickListener { onDelete(trip) }
    }
}
