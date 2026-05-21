package com.example.naikapa.presentation.home

import android.content.res.ColorStateList
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.naikapa.R
import com.example.naikapa.common.SessionManager
import com.example.naikapa.common.toast
import com.example.naikapa.databinding.FragmentHomeBinding
import com.google.android.material.card.MaterialCardView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    // List untuk mengelola visual mode transportasi
    private lateinit var modeCards: List<MaterialCardView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inisialisasi konfigurasi osmdroid User-Agent
        val ctx = requireContext().applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        // Mengubah sapaan di header dengan nama user
        val userName = sessionManager.getUserName() ?: "User NaikApa"
        binding.tvWelcomeUser.text = "Halo, $userName!"
        binding.tvWelcomeUser.visibility = View.VISIBLE

        initMap()
        setupTransitModes()
        setupRouteActions()
    }

    private fun initMap() {
        // Setup Tile Source kustom untuk CartoDB Positron (Light Mode)
        val positronTileSource = XYTileSource(
            "CartoDB_Positron",
            1, 19, 256, ".png",
            arrayOf(
                "https://a.basemaps.cartocdn.com/light_all/",
                "https://b.basemaps.cartocdn.com/light_all/",
                "https://c.basemaps.cartocdn.com/light_all/",
                "https://d.basemaps.cartocdn.com/light_all/"
            )
        )

        binding.mapView.apply {
            setTileSource(positronTileSource)
            setMultiTouchControls(true)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false

            // Set default view ke wilayah Jabodetabek (Jakarta Pusat sebagai titik jangkar)
            controller.apply {
                setZoom(12.5)
                val jakartaPoint = GeoPoint(-6.2088, 106.8456)
                setCenter(jakartaPoint)
            }
        }
    }

    private fun setupTransitModes() {
        // Daftarkan semua Card moda transit
        modeCards = listOf(
            binding.modeCampur,
            binding.modeTJ,
            binding.modeKRL,
            binding.modeMRT,
            binding.modeLRT,
            binding.modeMotor,
            binding.modeMobil
        )

        // Set klik listener untuk masing-masing card moda
        modeCards.forEach { card ->
            card.setOnClickListener {
                selectTransitMode(card)
            }
        }
    }

    private fun selectTransitMode(selectedCard: MaterialCardView) {
        modeCards.forEach { card ->
            val innerLayout = card.getChildAt(0) as ViewGroup
            val iconView = innerLayout.getChildAt(0) as ImageView
            val textView = innerLayout.getChildAt(1) as TextView

            if (card == selectedCard) {
                // Tampilan Aktif
                card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryLight))
                card.strokeColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                card.strokeWidth = dpToPx(1.5f)
                iconView.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
            } else {
                // Tampilan Inaktif
                card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
                card.strokeColor = ContextCompat.getColor(requireContext(), R.color.colorCardOutline)
                card.strokeWidth = dpToPx(1f)
                
                // Cari warna ikon default berdasarkan jenis modanya
                val defaultIconColor = when (card.id) {
                    R.id.modeTJ -> R.color.colorTransjakarta
                    R.id.modeKRL -> R.color.colorKRL
                    R.id.modeMRT -> R.color.colorMRT
                    R.id.modeLRT -> R.color.colorLRT
                    R.id.modeMotor -> R.color.colorMotor
                    R.id.modeMobil -> R.color.colorMobil
                    else -> R.color.colorTextSecondary
                }
                iconView.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), defaultIconColor))
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextPrimary))
            }
        }
    }

    private fun setupRouteActions() {
        // Tombol Swap Asal-Tujuan
        binding.btnSwap.setOnClickListener {
            val tempTitle = binding.tvOrigin.text.toString()
            val tempSub = binding.tvOriginSub.text.toString()

            binding.tvOrigin.text = binding.tvDestination.text
            binding.tvOriginSub.text = binding.tvDestinationSub.text

            binding.tvDestination.text = tempTitle
            binding.tvDestinationSub.text = tempSub

            toast("Rute asal-tujuan ditukar")
        }

        // Tombol GPS Lokasi
        binding.btnLocation.setOnClickListener {
            toast("Mendapatkan lokasi GPS...")
            binding.mapView.controller.animateTo(GeoPoint(-6.2088, 106.8456))
        }

        // Tombol Cari Rute
        binding.btnTemukanRute.setOnClickListener {
            val origin = binding.tvOrigin.text.toString()
            val destination = binding.tvDestination.text.toString()
            toast("Mencari rute dari $origin ke $destination...")
            
            // Animasikan panel rekomendasi agar terlihat dinamis
            binding.cardRecommendation.visibility = View.VISIBLE
            binding.cardRecommendation.alpha = 0f
            binding.cardRecommendation.animate().alpha(1f).setDuration(500).start()
        }

        // Notifikasi click
        binding.btnNotification.setOnClickListener {
            toast("Tidak ada notifikasi baru")
        }
    }

    private fun dpToPx(dp: Float): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
