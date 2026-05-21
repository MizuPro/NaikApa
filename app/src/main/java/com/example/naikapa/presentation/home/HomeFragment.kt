package com.example.naikapa.presentation.home

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.naikapa.common.SessionManager
import com.example.naikapa.databinding.FragmentHomeBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

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

        val userName = sessionManager.getUserName() ?: "User NaikApa"
        binding.tvWelcomeUser.text = userName

        initMap()
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
                setZoom(12.0)
                val jakartaPoint = GeoPoint(-6.2088, 106.8456)
                setCenter(jakartaPoint)
            }
        }
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
