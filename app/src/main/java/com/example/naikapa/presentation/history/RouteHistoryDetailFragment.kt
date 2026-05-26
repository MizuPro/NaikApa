package com.example.naikapa.presentation.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.naikapa.common.applyStatusBarTopMarginTo
import com.example.naikapa.common.toast
import com.example.naikapa.data.model.RouteHistory
import com.example.naikapa.databinding.FragmentRouteHistoryDetailBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RouteHistoryDetailFragment : Fragment() {

    private var _binding: FragmentRouteHistoryDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRouteHistoryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val history = RouteHistoryDetailSharedState.selected
        if (history == null) {
            toast("Data riwayat tidak ditemukan")
            findNavController().popBackStack()
            return
        }

        binding.root.applyStatusBarTopMarginTo(binding.cardToolbar)
        setupToolbar()
        bindData(history)
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun bindData(history: RouteHistory) {
        // Origin & Destination
        binding.tvOriginLabel.text = history.originName
        binding.tvDestinationLabel.text = history.destinationName

        // Mode · Priority
        binding.tvModeLabel.text = "${history.mode} · ${history.priority}"

        // Score
        binding.tvMatchScore.text = "${history.score}% Cocok"

        // Tanggal perjalanan
        binding.tvHistoryDate.text = formatDateTime(history.createdAt)

        // Metrics
        binding.tvDurationValue.text = formatDuration(history.estimatedTime)
        binding.tvFareValue.text = formatRupiah(history.estimatedCost)
        binding.tvWalkingValue.text = formatDistance(history.walkingDistance)
        binding.tvTransitValue.text = "${history.transitCount}x transit"

        // Alasan rekomendasi
        binding.tvReasonText.text = history.recommendationSummary.ifBlank {
            "Tidak ada keterangan tambahan untuk perjalanan ini."
        }
    }

    private fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("EEEE, d MMMM yyyy · HH:mm", Locale("id", "ID"))
        return sdf.format(Date(millis))
    }

    private fun formatDuration(seconds: Int): String {
        val minutes = (seconds + 59) / 60
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return if (hours > 0) {
            if (remainingMinutes > 0) "$hours j $remainingMinutes mnt" else "$hours j"
        } else {
            "$minutes mnt"
        }
    }

    private fun formatRupiah(value: Int): String {
        if (value == 0) return "Gratis"
        return "Rp " + String.format(Locale.US, "%,d", value).replace(",", ".")
    }

    private fun formatDistance(distanceMeters: Double): String {
        return if (distanceMeters >= 1000.0) {
            String.format(Locale.US, "%.1f km", distanceMeters / 1000.0)
        } else {
            "${distanceMeters.toInt()} m"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
