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
import com.example.naikapa.data.model.SearchHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SearchHistoryAdapter(
    private val onUseAsDestination: (SearchHistory) -> Unit,
    private val onDelete: (SearchHistory) -> Unit
) : ListAdapter<SearchHistory, SearchHistoryAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SearchHistory>() {
            override fun areItemsTheSame(a: SearchHistory, b: SearchHistory) = a.idSearch == b.idSearch
            override fun areContentsTheSame(a: SearchHistory, b: SearchHistory) = a == b
        }
        private val DATE_FORMAT = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSearchName: TextView = view.findViewById(R.id.tvSearchName)
        val tvSearchAddress: TextView = view.findViewById(R.id.tvSearchAddress)
        val tvSearchTime: TextView = view.findViewById(R.id.tvSearchTime)
        val btnUseAsDestination: ImageView = view.findViewById(R.id.btnUseAsDestination)
        val btnDeleteSearch: ImageView = view.findViewById(R.id.btnDeleteSearch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.tvSearchName.text = item.selectedName
        holder.tvSearchAddress.text = item.selectedAddress ?: item.keyword
        holder.tvSearchTime.text = DATE_FORMAT.format(Date(item.searchedAt))

        holder.btnUseAsDestination.setOnClickListener { onUseAsDestination(item) }
        holder.btnDeleteSearch.setOnClickListener { onDelete(item) }
    }
}
