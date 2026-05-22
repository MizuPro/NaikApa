package com.example.naikapa.presentation.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.naikapa.BuildConfig
import com.example.naikapa.R
import com.example.naikapa.common.applyStatusBarTopPadding
import com.example.naikapa.common.applyStatusBarTopMarginTo
import com.example.naikapa.common.AppConstants
import com.example.naikapa.common.SessionManager
import com.example.naikapa.common.toast
import com.example.naikapa.data.local.GtfsDao
import com.example.naikapa.data.local.HistoryDao
import com.example.naikapa.data.local.NaikApaDatabaseHelper
import com.example.naikapa.data.repository.GtfsStopSearchRepository
import com.example.naikapa.data.model.LocationPoint
import com.example.naikapa.data.model.MapMarkerType
import com.example.naikapa.data.model.MapPoint
import com.example.naikapa.data.model.MapStyle
import com.example.naikapa.data.model.CombinedRouteResult
import com.example.naikapa.data.model.PrivateVehicleMode
import com.example.naikapa.data.model.PrivateVehicleRouteResult
import com.example.naikapa.data.model.RouteStep
import com.example.naikapa.data.model.SearchHistory
import com.example.naikapa.data.model.SearchLocation
import com.example.naikapa.data.model.SortPreference
import com.example.naikapa.data.model.TransitMode
import com.example.naikapa.data.model.TransitRouteResult
import com.example.naikapa.data.remote.RemoteClient
import com.example.naikapa.data.repository.CombinedRouteRepository
import com.example.naikapa.data.repository.NearbyTransitStopRepository
import com.example.naikapa.data.repository.TransitGraphRepository
import com.example.naikapa.data.repository.TransitRoutingRepository
import com.example.naikapa.data.repository.TomTomRoutingRepository
import com.example.naikapa.data.repository.TomTomSearchRepository
import com.example.naikapa.databinding.FragmentHomeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.card.MaterialCardView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.example.naikapa.data.local.DisruptionReportDao
import com.example.naikapa.domain.recommendation.RecommendationEngine
import com.example.naikapa.domain.recommendation.RecommendationReasonBuilder
import com.example.naikapa.domain.recommendation.RecommendationScorer
import com.example.naikapa.data.model.RecommendationResult
import com.example.naikapa.data.model.ScoredRoute
import com.example.naikapa.data.model.VehicleTypeFilter
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.naikapa.data.model.RouteHistory
import com.example.naikapa.presentation.history.HistoryReplayRequest
import com.example.naikapa.presentation.route_detail.RouteDetailSharedState

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var dbHelper: NaikApaDatabaseHelper
    private lateinit var historyDao: HistoryDao
    private lateinit var disruptionReportDao: DisruptionReportDao
    private val searchRepository = TomTomSearchRepository(RemoteClient.tomTomSearchApi)
    private val tomTomRoutingRepository = TomTomRoutingRepository(RemoteClient.tomTomRoutingApi)
    private lateinit var gtfsSearchRepository: GtfsStopSearchRepository
    private lateinit var transitRoutingRepository: TransitRoutingRepository
    private lateinit var combinedRouteRepository: CombinedRouteRepository
    private lateinit var recommendationEngine: RecommendationEngine
    private lateinit var routeResultAdapter: RouteResultAdapter
    private lateinit var homeScope: CoroutineScope
    private var selectedModeCardId: Int = -1
    private var selectedVehicleTypeFilter: VehicleTypeFilter = VehicleTypeFilter.ALL
    private var selectedOrigin: LocationPoint?
        get() = savedOrigin
        set(value) {
            savedOrigin = value
        }
    private var selectedOriginStop: SearchLocation?
        get() = savedOriginStop
        set(value) {
            savedOriginStop = value
        }
    private var selectedDestination: SearchLocation?
        get() = savedDestination
        set(value) {
            savedDestination = value
        }
    private var destinationSearchJob: Job? = null
    private var originSearchJob: Job? = null
    private var currentMapStyle = MapStyle.POSITRON
    private val routeOverlays = mutableListOf<Overlay>()
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var isPanelVisible = true

    // Enum untuk melacak field mana yang sedang aktif di-edit
    private enum class ActiveSearchField { NONE, ORIGIN, DESTINATION }

    // List untuk mengelola visual mode transportasi
    private lateinit var modeCards: List<MaterialCardView>

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            fetchCurrentLocation()
        } else {
            toast(getString(R.string.location_permission_denied))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inisialisasi konfigurasi osmdroid User-Agent
        val ctx = requireContext().applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = AppConstants.MAP_USER_AGENT
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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        dbHelper = NaikApaDatabaseHelper(requireContext())
        historyDao = HistoryDao(dbHelper)
        val gtfsDao = GtfsDao(dbHelper)
        gtfsSearchRepository = GtfsStopSearchRepository(gtfsDao)
        transitRoutingRepository = TransitRoutingRepository(TransitGraphRepository(gtfsDao))
        combinedRouteRepository = CombinedRouteRepository(
            nearbyTransitStopRepository = NearbyTransitStopRepository(gtfsDao),
            tomTomRoutingRepository = tomTomRoutingRepository,
            transitRoutingRepository = transitRoutingRepository,
            apiKey = BuildConfig.TOMTOM_API_KEY
        )
        homeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        // Inisialisasi DisruptionReportDao dan RecommendationEngine
        disruptionReportDao = DisruptionReportDao(dbHelper)
        recommendationEngine = RecommendationEngine(
            transitRoutingRepository = transitRoutingRepository,
            tomTomRoutingRepository = tomTomRoutingRepository,
            combinedRouteRepository = combinedRouteRepository,
            disruptionReportDao = disruptionReportDao,
            scorer = RecommendationScorer(),
            reasonBuilder = RecommendationReasonBuilder()
        )

        // Setup RecyclerView rekomendasi
        routeResultAdapter = RouteResultAdapter(requireContext())
        routeResultAdapter.onItemClick = { scoredRoute ->
            com.example.naikapa.presentation.route_detail.RouteDetailSharedState.selectedRoute = scoredRoute
            com.example.naikapa.presentation.route_detail.RouteDetailSharedState.origin = selectedOrigin
            com.example.naikapa.presentation.route_detail.RouteDetailSharedState.destination = selectedDestination
            androidx.navigation.fragment.NavHostFragment.findNavController(this@HomeFragment).navigate(
                R.id.action_homeFragment_to_routeDetailFragment
            )
        }
        binding.rvRecommendations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = routeResultAdapter
            isNestedScrollingEnabled = false
        }

        // Mengubah sapaan di header dengan nama user
        val userName = sessionManager.getUserName() ?: "User NaikApa"
        binding.tvWelcomeUser.text = "Halo, $userName!"
        binding.tvWelcomeUser.visibility = View.VISIBLE

        initMap()
        setupTransitModes()
        setupVehicleTypeFilter()
        setupSortChips()
        setupOriginSearch()
        setupDestinationSearch()
        setupRouteActions()
        setupPanelToggle()
        
        binding.root.applyStatusBarTopMarginTo(binding.cardHeader, dpToPx(16f))
        setupBottomSheetScrim()
        restoreSearchFieldsState()
    }

    private fun initMap() {
        binding.mapView.apply {
            setMultiTouchControls(true)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false

            // Set default view ke wilayah Jabodetabek (Jakarta Pusat sebagai titik jangkar)
            controller.apply {
                setZoom(AppConstants.MAP_DEFAULT_ZOOM)
                val jakartaPoint = GeoPoint(AppConstants.MAP_DEFAULT_LAT, AppConstants.MAP_DEFAULT_LON)
                setCenter(jakartaPoint)
            }
        }
        setMapStyle(MapStyle.POSITRON)
    }

    private fun setMapStyle(style: MapStyle) {
        currentMapStyle = style
        binding.mapView.setTileSource(createTileSource(style))
        binding.btnMapStyle.text = when (style) {
            MapStyle.POSITRON -> getString(R.string.map_style_light)
            MapStyle.DARK_MATTER -> getString(R.string.map_style_dark)
        }
        binding.mapView.invalidate()
    }

    private fun toggleMapStyle() {
        val nextStyle = when (currentMapStyle) {
            MapStyle.POSITRON -> MapStyle.DARK_MATTER
            MapStyle.DARK_MATTER -> MapStyle.POSITRON
        }
        setMapStyle(nextStyle)
    }

    private fun createTileSource(style: MapStyle): XYTileSource {
        val (name, hosts) = when (style) {
            MapStyle.POSITRON -> AppConstants.MAP_TILE_POSITRON to arrayOf(
                "https://a.basemaps.cartocdn.com/light_all/",
                "https://b.basemaps.cartocdn.com/light_all/",
                "https://c.basemaps.cartocdn.com/light_all/",
                "https://d.basemaps.cartocdn.com/light_all/"
            )
            MapStyle.DARK_MATTER -> AppConstants.MAP_TILE_DARK_MATTER to arrayOf(
                "https://a.basemaps.cartocdn.com/dark_all/",
                "https://b.basemaps.cartocdn.com/dark_all/",
                "https://c.basemaps.cartocdn.com/dark_all/",
                "https://d.basemaps.cartocdn.com/dark_all/"
            )
        }
        return XYTileSource(name, 1, 19, 256, ".png", hosts)
    }

    private fun setupTransitModes() {
        binding.chipGroupModes.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                selectedModeCardId = checkedIds.first()
            }
        }
        selectedModeCardId = R.id.modeCampur
    }

    private fun setupVehicleTypeFilter() {
        binding.chipGroupVehicleFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                selectedVehicleTypeFilter = when (checkedIds.first()) {
                    R.id.filterTransumSaja -> VehicleTypeFilter.TRANSIT_ONLY
                    R.id.filterKendaraanPribadiSaja -> VehicleTypeFilter.PRIVATE_ONLY
                    else -> VehicleTypeFilter.ALL
                }
                applyVehicleFilterToModeChips()
            }
        }
        selectedVehicleTypeFilter = VehicleTypeFilter.ALL
    }

    /**
     * Sembunyikan/tampilkan chip moda yang tidak relevan berdasarkan filter tipe kendaraan.
     * - TRANSIT_ONLY: sembunyikan chip Motor dan Mobil
     * - PRIVATE_ONLY: sembunyikan chip TJ, KRL, MRT, LRT
     * - ALL: tampilkan semua chip
     */
    private fun applyVehicleFilterToModeChips() {
        val transitChipIds = listOf(R.id.modeTJ, R.id.modeKRL, R.id.modeMRT, R.id.modeLRT)
        val privateChipIds = listOf(R.id.modeMotor, R.id.modeMobil)

        when (selectedVehicleTypeFilter) {
            VehicleTypeFilter.TRANSIT_ONLY -> {
                transitChipIds.forEach { id ->
                    binding.chipGroupModes.findViewById<com.google.android.material.chip.Chip>(id)?.visibility = View.VISIBLE
                }
                privateChipIds.forEach { id ->
                    binding.chipGroupModes.findViewById<com.google.android.material.chip.Chip>(id)?.visibility = View.GONE
                }
                // Jika chip yang sedang dipilih adalah motor/mobil, reset ke Campur
                if (selectedModeCardId == R.id.modeMotor || selectedModeCardId == R.id.modeMobil) {
                    binding.modeCampur.isChecked = true
                    selectedModeCardId = R.id.modeCampur
                }
            }
            VehicleTypeFilter.PRIVATE_ONLY -> {
                transitChipIds.forEach { id ->
                    binding.chipGroupModes.findViewById<com.google.android.material.chip.Chip>(id)?.visibility = View.GONE
                }
                privateChipIds.forEach { id ->
                    binding.chipGroupModes.findViewById<com.google.android.material.chip.Chip>(id)?.visibility = View.VISIBLE
                }
                // Jika chip yang sedang dipilih adalah transit, reset ke Campur
                if (transitChipIds.contains(selectedModeCardId)) {
                    binding.modeCampur.isChecked = true
                    selectedModeCardId = R.id.modeCampur
                }
            }
            VehicleTypeFilter.ALL -> {
                (transitChipIds + privateChipIds).forEach { id ->
                    binding.chipGroupModes.findViewById<com.google.android.material.chip.Chip>(id)?.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupDestinationSearch() {
        // Klik pada area tujuan (tvDestination/tvDestinationSub) → aktifkan input tujuan
        binding.layoutDestinationField.setOnClickListener {
            activateDestinationSearch()
        }
        binding.tvDestination.setOnClickListener {
            activateDestinationSearch()
        }
        binding.tvDestinationSub.setOnClickListener {
            activateDestinationSearch()
        }

        binding.etDestinationSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                scheduleDestinationSearch(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun activateDestinationSearch() {
        binding.tilDestinationSearch.visibility = View.VISIBLE
        binding.etDestinationSearch.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(binding.etDestinationSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        // Tutup origin search jika sedang terbuka
        binding.tilOriginSearch.visibility = View.GONE
        hideSearchResults()
    }

    private fun setupOriginSearch() {
        // Klik pada area asal (tvOrigin/tvOriginSub) → aktifkan input asal
        binding.layoutOriginField.setOnClickListener {
            activateOriginSearch()
        }
        binding.tvOrigin.setOnClickListener {
            activateOriginSearch()
        }
        binding.tvOriginSub.setOnClickListener {
            activateOriginSearch()
        }

        binding.etOriginSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                scheduleOriginSearch(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun activateOriginSearch() {
        binding.tilOriginSearch.visibility = View.VISIBLE
        binding.etOriginSearch.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(binding.etOriginSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        // Tutup destination search jika sedang terbuka
        binding.tilDestinationSearch.visibility = View.GONE
        hideSearchResults()
    }

    private fun scheduleOriginSearch(query: String) {
        originSearchJob?.cancel()
        val trimmedQuery = query.trim()
        if (trimmedQuery.length < AppConstants.GTFS_MIN_QUERY_LENGTH) {
            hideSearchResults()
            return
        }

        originSearchJob = homeScope.launch {
            delay(AppConstants.TOMTOM_SEARCH_DEBOUNCE_MS)
            performOriginSearch(trimmedQuery)
        }
    }

    private suspend fun performOriginSearch(query: String) {
        showSearchStatus(getString(R.string.search_destination_loading), isError = false)

        val latBias = AppConstants.MAP_DEFAULT_LAT
        val lonBias = AppConstants.MAP_DEFAULT_LON
        val combined = mutableListOf<SearchLocation>()

        coroutineScope {
            val gtfsDeferred = async(Dispatchers.IO) {
                gtfsSearchRepository.search(query, null, null, null)
            }
            val tomtomDeferred = if (BuildConfig.TOMTOM_API_KEY != AppConstants.TOMTOM_API_KEY_PLACEHOLDER) {
                async(Dispatchers.IO) {
                    searchRepository.search(
                        query = query,
                        apiKey = BuildConfig.TOMTOM_API_KEY,
                        latitudeBias = latBias,
                        longitudeBias = lonBias
                    )
                }
            } else null

            combined.addAll(gtfsDeferred.await())
            tomtomDeferred?.await()?.onSuccess { locations ->
                combined.addAll(locations.take(AppConstants.TOMTOM_SEARCH_LIMIT))
            }
        }

        if (combined.isEmpty()) {
            showSearchStatus(getString(R.string.search_combined_empty), isError = false)
        } else {
            renderOriginSearchResults(query, combined)
        }
    }

    private fun renderOriginSearchResults(query: String, locations: List<SearchLocation>) {
        binding.tvSearchStatus.visibility = View.GONE
        binding.layoutSearchResults.removeAllViews()
        locations.take(AppConstants.TOMTOM_SEARCH_LIMIT).forEach { location ->
            binding.layoutSearchResults.addView(createOriginResultView(query, location))
        }
        binding.layoutSearchResults.visibility = View.VISIBLE
    }

    private fun createOriginResultView(query: String, location: SearchLocation): View {
        val isGtfs = location.source == SearchLocation.SOURCE_GTFS
        val card = MaterialCardView(requireContext()).apply {
            radius = dpToPx(10f).toFloat()
            cardElevation = 0f
            strokeWidth = dpToPx(1f)
            strokeColor = ContextCompat.getColor(
                requireContext(),
                if (isGtfs) R.color.colorPrimary else R.color.colorCardOutline
            )
            setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            setOnClickListener { selectOriginFromSearch(query, location) }
        }
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(12f), dpToPx(9f), dpToPx(12f), dpToPx(9f))
        }
        val title = TextView(requireContext()).apply {
            text = location.name
            setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextPrimary))
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val subtitle = TextView(requireContext()).apply {
            text = location.address ?: formatCoordinates(location.latitude, location.longitude)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextSecondary))
            textSize = 11f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        content.addView(title)
        content.addView(subtitle)
        if (isGtfs) {
            val badge = TextView(requireContext()).apply {
                text = GtfsStopSearchRepository.agencyIdToLabel(location.agencyId)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
                textSize = 10f
                setPadding(dpToPx(6f), dpToPx(2f), dpToPx(6f), dpToPx(2f))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryLight))
                    cornerRadius = dpToPx(4f).toFloat()
                    setStroke(dpToPx(1f), ContextCompat.getColor(requireContext(), R.color.colorPrimary))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dpToPx(4f) }
            }
            content.addView(badge)
        }
        card.addView(content)
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dpToPx(8f) }
        return card
    }

    private fun selectOriginFromSearch(query: String, location: SearchLocation) {
        originSearchJob?.cancel()
        selectedOrigin = LocationPoint(
            label = location.name,
            latitude = location.latitude,
            longitude = location.longitude,
            isFromGps = false
        )
        selectedOriginStop = location
        binding.tvOrigin.text = location.name
        binding.tvOriginSub.text = location.address ?: formatCoordinates(location.latitude, location.longitude)
        binding.etOriginSearch.setText("")
        binding.tilOriginSearch.visibility = View.GONE
        hideSearchResults()

        val originPoint = MapPoint(
            label = getString(R.string.map_marker_origin),
            latitude = location.latitude,
            longitude = location.longitude,
            description = location.name,
            markerType = MapMarkerType.ORIGIN
        )
        showOriginMarker(originPoint)
        binding.mapView.controller.apply {
            setZoom(AppConstants.MAP_LOCATION_ZOOM)
            animateTo(pointToGeoPoint(originPoint))
        }
        // Sembunyikan keyboard
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.etOriginSearch.windowToken, 0)
    }

    private fun setupSortChips() {
        binding.chipGroupCriteria.setOnCheckedStateChangeListener { group, checkedIds ->
            if (selectedOrigin != null && selectedDestination != null && binding.scrollRecommendations.visibility == View.VISIBLE) {
                handleFindRouteClick()
            }
        }
    }

    private fun scheduleDestinationSearch(query: String) {
        destinationSearchJob?.cancel()
        val trimmedQuery = query.trim()
        // Gunakan GTFS_MIN_QUERY_LENGTH (2) agar pencarian lokal bisa berjalan lebih awal
        if (trimmedQuery.length < AppConstants.GTFS_MIN_QUERY_LENGTH) {
            hideSearchResults()
            return
        }

        destinationSearchJob = homeScope.launch {
            delay(AppConstants.TOMTOM_SEARCH_DEBOUNCE_MS)
            performDestinationSearch(trimmedQuery)
        }
    }

    private suspend fun performDestinationSearch(query: String) {
        showSearchStatus(getString(R.string.search_destination_loading), isError = false)

        val userLat = selectedOrigin?.latitude
        val userLon = selectedOrigin?.longitude
        val latBias = userLat ?: AppConstants.MAP_DEFAULT_LAT
        val lonBias = userLon ?: AppConstants.MAP_DEFAULT_LON
        val agencyFilter = getAgencyFilterForCurrentMode()

        val combined = mutableListOf<SearchLocation>()

        coroutineScope {
            // GTFS lokal — selalu jalan, offline, tanpa API key
            val gtfsDeferred = async(Dispatchers.IO) {
                gtfsSearchRepository.search(query, userLat, userLon, agencyFilter)
            }

            // TomTom — hanya jika API key valid
            val tomtomDeferred = if (BuildConfig.TOMTOM_API_KEY != AppConstants.TOMTOM_API_KEY_PLACEHOLDER) {
                async(Dispatchers.IO) {
                    searchRepository.search(
                        query = query,
                        apiKey = BuildConfig.TOMTOM_API_KEY,
                        latitudeBias = latBias,
                        longitudeBias = lonBias
                    )
                }
            } else null

            // GTFS selalu tampil (error TomTom tidak memblokir)
            combined.addAll(gtfsDeferred.await())

            tomtomDeferred?.await()?.onSuccess { locations ->
                combined.addAll(locations.take(AppConstants.TOMTOM_SEARCH_LIMIT))
            }
        }

        if (combined.isEmpty()) {
            showSearchStatus(getString(R.string.search_combined_empty), isError = false)
        } else {
            renderSearchResults(query, combined)
        }
    }

    private fun renderSearchResults(query: String, locations: List<SearchLocation>) {
        binding.tvSearchStatus.visibility = View.GONE
        binding.layoutSearchResults.removeAllViews()
        locations.take(AppConstants.TOMTOM_SEARCH_LIMIT).forEach { location ->
            binding.layoutSearchResults.addView(createSearchResultView(query, location))
        }
        binding.layoutSearchResults.visibility = View.VISIBLE
    }

    private fun createSearchResultView(query: String, location: SearchLocation): View {
        val isGtfs = location.source == SearchLocation.SOURCE_GTFS
        val card = MaterialCardView(requireContext()).apply {
            radius = dpToPx(10f).toFloat()
            cardElevation = 0f
            strokeWidth = dpToPx(1f)
            strokeColor = ContextCompat.getColor(
                requireContext(),
                if (isGtfs) R.color.colorPrimary else R.color.colorCardOutline
            )
            setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            setOnClickListener { selectDestination(query, location) }
        }
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(12f), dpToPx(9f), dpToPx(12f), dpToPx(9f))
        }
        val title = TextView(requireContext()).apply {
            text = location.name
            setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextPrimary))
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val subtitle = TextView(requireContext()).apply {
            text = location.address ?: formatCoordinates(location.latitude, location.longitude)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextSecondary))
            textSize = 11f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        content.addView(title)
        content.addView(subtitle)

        // Badge agency/moda khusus untuk hasil GTFS lokal
        if (isGtfs) {
            val badge = TextView(requireContext()).apply {
                text = GtfsStopSearchRepository.agencyIdToLabel(location.agencyId)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
                textSize = 10f
                setPadding(dpToPx(6f), dpToPx(2f), dpToPx(6f), dpToPx(2f))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryLight))
                    cornerRadius = dpToPx(4f).toFloat()
                    setStroke(
                        dpToPx(1f),
                        ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                    )
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dpToPx(4f) }
            }
            content.addView(badge)
        }

        card.addView(content)
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dpToPx(8f)
        }
        return card
    }

    /** Kembalikan agencyId GTFS sesuai moda transit yang dipilih user, atau null untuk semua moda. */
    private fun getAgencyFilterForCurrentMode(): String? = when (selectedModeCardId) {
        R.id.modeTJ  -> "tj"
        R.id.modeKRL -> "krl"
        R.id.modeMRT -> "mrt"
        R.id.modeLRT -> "lrt"
        else         -> null  // modeCampur, modeMotor, modeMobil → tampilkan semua stop
    }

    private fun selectDestination(query: String, location: SearchLocation) {
        selectedDestination = location
        destinationSearchJob?.cancel()
        binding.tvDestination.text = location.name
        binding.tvDestinationSub.text = location.address ?: formatCoordinates(location.latitude, location.longitude)
        binding.etDestinationSearch.setText("")
        binding.tilDestinationSearch.visibility = View.GONE
        hideSearchResults()

        val destinationPoint = MapPoint(
            label = getString(R.string.map_marker_destination),
            latitude = location.latitude,
            longitude = location.longitude,
            description = location.name,
            markerType = MapMarkerType.DESTINATION
        )
        showDestinationMarker(destinationPoint)
        binding.mapView.controller.apply {
            setZoom(AppConstants.MAP_LOCATION_ZOOM)
            animateTo(pointToGeoPoint(destinationPoint))
        }
        saveSearchHistory(query, location)
        // Sembunyikan keyboard
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.etDestinationSearch.windowToken, 0)
        toast(getString(R.string.search_destination_selected))
    }

    private fun saveSearchHistory(query: String, location: SearchLocation) {
        val userId = sessionManager.getUserId()
        if (userId <= 0) return
        historyDao.insertSearchHistory(
            SearchHistory(
                idUser = userId,
                keyword = query,
                selectedName = location.name,
                selectedAddress = location.address,
                selectedLat = location.latitude,
                selectedLon = location.longitude
            )
        )
    }

    private fun hideSearchResults() {
        binding.tvSearchStatus.visibility = View.GONE
        binding.layoutSearchResults.visibility = View.GONE
        binding.layoutSearchResults.removeAllViews()
    }

    private fun showSearchStatus(message: String, isError: Boolean) {
        binding.layoutSearchResults.visibility = View.GONE
        binding.layoutSearchResults.removeAllViews()
        binding.tvSearchStatus.text = message
        binding.tvSearchStatus.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isError) R.color.colorError else R.color.colorTextSecondary
            )
        )
        binding.tvSearchStatus.visibility = View.VISIBLE
    }

    private fun showLoadingState(loadingText: String) {
        binding.btnTemukanRute.visibility = View.INVISIBLE
        binding.progressRouteSearch.visibility = View.VISIBLE
        binding.tvEmptyState.text = loadingText
        binding.tvEmptyState.visibility = View.VISIBLE
        binding.scrollRecommendations.visibility = View.GONE
        binding.tvPriorityLabel.visibility = View.GONE
        binding.cardPriorityBadge.visibility = View.GONE
        binding.btnCloseRecommendation.visibility = View.GONE
        binding.cardRecommendation.visibility = View.VISIBLE
    }

    private fun hideLoadingState() {
        binding.btnTemukanRute.visibility = View.VISIBLE
        binding.progressRouteSearch.visibility = View.GONE
    }

    private fun showSearchErrorState(message: String) {
        toast(message)
        binding.tvEmptyState.text = message
        binding.tvEmptyState.visibility = View.VISIBLE
        binding.scrollRecommendations.visibility = View.GONE
        binding.tvPriorityLabel.visibility = View.GONE
        binding.cardPriorityBadge.visibility = View.GONE
        binding.btnCloseRecommendation.visibility = View.GONE
        binding.cardRecommendation.visibility = View.VISIBLE
    }

    private fun setupRouteActions() {
        // Tombol Swap Asal-Tujuan
        binding.btnSwap.setOnClickListener {
            val tempTitle = binding.tvOrigin.text.toString()
            val tempSub = binding.tvOriginSub.text.toString()
            val tempOriginStop = selectedOriginStop

            binding.tvOrigin.text = binding.tvDestination.text
            binding.tvOriginSub.text = binding.tvDestinationSub.text

            binding.tvDestination.text = tempTitle
            binding.tvDestinationSub.text = tempSub
            selectedOriginStop = selectedDestination
            selectedDestination = tempOriginStop
            selectedOriginStop?.let { location ->
                selectedOrigin = LocationPoint(
                    label = location.name,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    isFromGps = false
                )
            }

            toast("Rute asal-tujuan ditukar")
        }

        // Tombol GPS Lokasi
        binding.btnLocation.setOnClickListener {
            startLocationFlow()
        }

        binding.btnMapStyle.setOnClickListener {
            toggleMapStyle()
        }

        // Tombol Cari Rute
        binding.btnTemukanRute.setOnClickListener {
            handleFindRouteClick()
        }

        // Notifikasi click
        binding.btnNotification.setOnClickListener {
            toast("Tidak ada notifikasi baru")
        }

        // Tombol tutup rekomendasi
        binding.btnCloseRecommendation.setOnClickListener {
            resetRecommendation()
        }
    }

    private fun setupPanelToggle() {
        binding.btnCollapsePanel.setOnClickListener {
            hidePanel()
        }
        binding.btnShowPanel.setOnClickListener {
            showPanel()
        }
    }

    private fun hidePanel() {
        isPanelVisible = false
        binding.cardSearch.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.cardSearch.visibility = View.GONE
                binding.btnShowPanel.visibility = View.VISIBLE
                binding.btnShowPanel.alpha = 0f
                binding.btnShowPanel.animate().alpha(1f).setDuration(200).start()
            }
            .start()
        // Sembunyikan keyboard jika ada
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun showPanel() {
        isPanelVisible = true
        binding.btnShowPanel.animate()
            .alpha(0f)
            .setDuration(150)
            .withEndAction {
                binding.btnShowPanel.visibility = View.GONE
                binding.cardSearch.visibility = View.VISIBLE
                binding.cardSearch.alpha = 0f
                binding.cardSearch.animate().alpha(1f).setDuration(200).start()
            }
            .start()
    }

    private fun handleFindRouteClick() {
        val origin = selectedOrigin
        val destination = selectedDestination

        if (origin == null || destination == null) {
            toast(getString(R.string.home_validation_select_origin_and_destination))
            return
        }

        val sortPreference = getSelectedSortPreference()
        val transitMode = getTransitModeForCurrentMode()
        val uid = sessionManager.getUserId()
        val hasMotor = if (uid > 0) {
            try { com.example.naikapa.data.local.UserDao(dbHelper).getUserById(uid)?.hasMotor ?: false }
            catch (e: Exception) { false }
        } else false
        val hasCar = if (uid > 0) {
            try { com.example.naikapa.data.local.UserDao(dbHelper).getUserById(uid)?.hasCar ?: false }
            catch (e: Exception) { false }
        } else false

        // Validasi khusus filter Kendaraan Pribadi Saja
        if (selectedVehicleTypeFilter == VehicleTypeFilter.PRIVATE_ONLY) {
            val apiKeyValid = BuildConfig.TOMTOM_API_KEY != AppConstants.TOMTOM_API_KEY_PLACEHOLDER
            if (!apiKeyValid) {
                toast(getString(R.string.route_private_api_key_missing))
                return
            }
            if (!hasMotor && !hasCar) {
                toast("Kamu belum mendaftarkan kendaraan pribadi. Perbarui profil untuk menambahkan motor atau mobil.")
                return
            }
            // Peringatan jika hanya punya salah satu kendaraan
            if (!hasMotor && hasCar) {
                toast("Kamu tidak memiliki motor. Hanya rute mobil yang akan ditampilkan.")
            } else if (hasMotor && !hasCar) {
                toast("Kamu tidak memiliki mobil. Hanya rute motor yang akan ditampilkan.")
            }
        }

        // Jika mode chip moda adalah kendaraan pribadi spesifik (Motor/Mobil), gunakan flow lama
        getSelectedPrivateVehicleMode()?.let { mode ->
            handlePrivateVehicleRouteClick(mode)
            return
        }

        showLoadingState(getString(R.string.route_recommendation_loading))

        homeScope.launch {
            val result = withContext(Dispatchers.IO) {
                recommendationEngine.recommend(
                    origin = origin,
                    destination = destination,
                    transitMode = transitMode,
                    sortPreference = sortPreference,
                    hasMotor = hasMotor && BuildConfig.TOMTOM_API_KEY != AppConstants.TOMTOM_API_KEY_PLACEHOLDER,
                    hasCar = hasCar && BuildConfig.TOMTOM_API_KEY != AppConstants.TOMTOM_API_KEY_PLACEHOLDER,
                    tomTomApiKey = BuildConfig.TOMTOM_API_KEY,
                    vehicleTypeFilter = selectedVehicleTypeFilter,
                    originStopId = selectedOriginStop?.stopId,
                    destinationStopId = selectedDestination?.stopId
                )
            }
            result
                .onSuccess { recommendation ->
                    hideLoadingState()
                    showRecommendationResult(origin, destination, recommendation)
                }
                .onFailure {
                    // Fallback ke flow lama jika engine gagal
                    val originStopId = selectedOriginStop?.stopId
                    val destinationStopId = selectedDestination?.stopId
                    if (!originStopId.isNullOrBlank() && !destinationStopId.isNullOrBlank()) {
                        val transitResult = withContext(Dispatchers.IO) {
                            transitRoutingRepository.findRoute(originStopId, destinationStopId, transitMode, sortPreference)
                        }
                        hideLoadingState()
                        if (transitResult != null) showTransitRouteResult(transitResult)
                        else {
                            showSearchErrorState(getString(R.string.route_transit_not_found))
                        }
                    } else {
                        hideLoadingState()
                        showSearchErrorState(getString(R.string.route_combined_not_found))
                    }
                }
        }
    }

    private fun handleCombinedRouteClick() {
        val origin = selectedOrigin
        val destination = selectedDestination
        if (origin == null || destination == null) {
            toast(getString(R.string.home_validation_select_origin_and_destination))
            return
        }
        if (BuildConfig.TOMTOM_API_KEY == AppConstants.TOMTOM_API_KEY_PLACEHOLDER) {
            toast(getString(R.string.route_private_api_key_missing))
            return
        }

        showLoadingState(getString(R.string.route_combined_loading))
        homeScope.launch {
            val result = withContext(Dispatchers.IO) {
                combinedRouteRepository.findCombinedRoutes(
                    originLat = origin.latitude,
                    originLon = origin.longitude,
                    destinationLat = destination.latitude,
                    destinationLon = destination.longitude,
                    privateVehicleMode = getCombinedPrivateVehicleMode(),
                    transitMode = getTransitModeForCurrentMode(),
                    sortPreference = getSelectedSortPreference(),
                    agencyId = getAgencyFilterForCurrentMode()
                )
            }
            hideLoadingState()
            result
                .onSuccess { routes ->
                    val mainRoute = routes.firstOrNull()
                    if (mainRoute == null) {
                        showSearchErrorState(getString(R.string.route_combined_not_found))
                    } else {
                        showCombinedRouteResult(origin, destination, mainRoute)
                    }
                }
                .onFailure {
                    showSearchErrorState(getString(R.string.route_combined_error))
                }
        }
    }

    private fun handlePrivateVehicleRouteClick(mode: PrivateVehicleMode) {
        val origin = selectedOrigin
        val destination = selectedDestination
        if (origin == null || destination == null) {
            toast(getString(R.string.home_validation_select_origin_and_destination))
            return
        }
        if (BuildConfig.TOMTOM_API_KEY == AppConstants.TOMTOM_API_KEY_PLACEHOLDER) {
            toast(getString(R.string.route_private_api_key_missing))
            return
        }

        showLoadingState(getString(R.string.route_private_loading))
        homeScope.launch {
            val result = withContext(Dispatchers.IO) {
                tomTomRoutingRepository.calculateRoute(
                    originLat = origin.latitude,
                    originLon = origin.longitude,
                    destinationLat = destination.latitude,
                    destinationLon = destination.longitude,
                    mode = mode,
                    apiKey = BuildConfig.TOMTOM_API_KEY
                )
            }
            hideLoadingState()
            result
                .onSuccess { routes ->
                    val mainRoute = routes.firstOrNull()
                    if (mainRoute == null) {
                        showSearchErrorState(getString(R.string.route_private_not_found))
                    } else {
                        showPrivateVehicleRouteResult(origin, destination, mainRoute)
                    }
                }
                .onFailure {
                    showSearchErrorState(getString(R.string.route_private_error))
                }
        }
    }

    private fun showRecommendationCard() {
        binding.tvEmptyState.visibility = View.GONE
        binding.scrollRecommendations.visibility = View.VISIBLE
        binding.tvPriorityLabel.visibility = View.VISIBLE
        binding.cardPriorityBadge.visibility = View.VISIBLE
        binding.btnCloseRecommendation.visibility = View.VISIBLE
        binding.cardRecommendation.visibility = View.VISIBLE
        binding.cardRecommendation.alpha = 0f
        binding.cardRecommendation.animate().alpha(1f).setDuration(500).start()
    }

    private fun resetRecommendation() {
        // Jika kontainer rekomendasi sudah tersembunyi (dalam keadaan kosong), sembunyikan cardRecommendation sepenuhnya
        if (binding.scrollRecommendations.visibility == View.GONE) {
            binding.cardRecommendation.visibility = View.GONE
            return
        }

        // Reset state rekomendasi ke empty state
        routeResultAdapter.submitList(emptyList())
        binding.scrollRecommendations.visibility = View.GONE
        binding.tvPriorityLabel.visibility = View.GONE
        binding.cardPriorityBadge.visibility = View.GONE
        binding.btnCloseRecommendation.visibility = View.VISIBLE // Tetap biarkan VISIBLE agar tombol "X" bisa diklik untuk menyembunyikan panel sepenuhnya
        binding.tvEmptyState.text = getString(R.string.home_empty_state_text)
        binding.tvEmptyState.visibility = View.VISIBLE
        // Bersihkan rute di peta
        clearRouteOverlays()
        originMarker?.let { binding.mapView.overlays.remove(it) }
        destinationMarker?.let { binding.mapView.overlays.remove(it) }
        originMarker = null
        destinationMarker = null
        binding.mapView.invalidate()
    }

    /**
     * Menampilkan hasil RecommendationEngine: update RecyclerView, peta, dan label prioritas.
     */
    private fun showRecommendationResult(
        origin: LocationPoint,
        destination: SearchLocation,
        recommendation: RecommendationResult
    ) {
        // Update label prioritas
        binding.tvPriorityLabel.text = when (recommendation.sortPreference) {
            com.example.naikapa.data.model.SortPreference.FASTEST          -> "TERCEPAT"
            com.example.naikapa.data.model.SortPreference.CHEAPEST         -> "TERHEMAT"
            com.example.naikapa.data.model.SortPreference.MIN_WALKING      -> "MINIM JALAN KAKI"
            com.example.naikapa.data.model.SortPreference.FEWEST_TRANSFERS -> "MINIM TRANSIT"
        }

        // Submit daftar ke adapter
        routeResultAdapter.submitList(recommendation.all)

        // Tampilkan peta berdasarkan rekomendasi utama
        val mainCandidate = recommendation.main.candidate
        clearRouteOverlays()
        val originPoint = MapPoint(
            label = getString(R.string.map_marker_origin),
            latitude = origin.latitude,
            longitude = origin.longitude,
            description = origin.label,
            markerType = MapMarkerType.ORIGIN
        )
        val destinationPoint = MapPoint(
            label = getString(R.string.map_marker_destination),
            latitude = destination.latitude,
            longitude = destination.longitude,
            description = destination.name,
            markerType = MapMarkerType.DESTINATION
        )
        showOriginMarker(originPoint)
        showDestinationMarker(destinationPoint)

        when (mainCandidate) {
            is com.example.naikapa.data.model.RouteCandidate.Transit -> {
                val routePoints = mainCandidate.result.steps.toMapPoints()
                if (routePoints.size > 1) {
                    showTransitMarkers(routePoints.drop(1).dropLast(1))
                    drawRoutePolyline(routePoints, ContextCompat.getColor(requireContext(), R.color.colorPrimary))
                }
            }
            is com.example.naikapa.data.model.RouteCandidate.PrivateVehicle -> {
                val color = if (mainCandidate.result.mode == PrivateVehicleMode.MOTOR)
                    ContextCompat.getColor(requireContext(), R.color.colorMotor)
                else ContextCompat.getColor(requireContext(), R.color.colorMobil)
                drawRoutePolyline(mainCandidate.result.points, color)
            }
            is com.example.naikapa.data.model.RouteCandidate.Combined -> {
                val vehicleResult = mainCandidate.result.privateVehicleResult
                val transitResult = mainCandidate.result.transitResult
                if (vehicleResult != null) {
                    val color = if (vehicleResult.mode == PrivateVehicleMode.MOTOR)
                        ContextCompat.getColor(requireContext(), R.color.colorMotor)
                    else ContextCompat.getColor(requireContext(), R.color.colorMobil)
                    drawRoutePolyline(vehicleResult.points, color)
                }
                if (transitResult != null) {
                    val transitPoints = transitResult.steps.toMapPoints()
                    if (transitPoints.size > 1) {
                        showTransitMarkers(transitPoints.drop(1).dropLast(1))
                        drawRoutePolyline(transitPoints, ContextCompat.getColor(requireContext(), R.color.colorPrimary))
                    }
                }
            }
        }

        binding.mapView.controller.apply {
            setZoom(11.0)
            animateTo(GeoPoint(origin.latitude, origin.longitude))
        }

        showRecommendationCard()
        toast(getString(R.string.route_recommendation_ready))

        // Simpan riwayat perjalanan untuk rekomendasi utama
        val priorityLabel = when (recommendation.sortPreference) {
            SortPreference.FASTEST -> "Tercepat"
            SortPreference.CHEAPEST -> "Terhemat"
            SortPreference.MIN_WALKING -> "Minim Jalan Kaki"
            SortPreference.FEWEST_TRANSFERS -> "Minim Transit"
        }
        val modeLabel = getModeLabel()
        saveRouteHistory(origin, destination, recommendation.main, modeLabel, priorityLabel)

        // Simpan mode/priority ke SharedState untuk dipakai saat simpan favorit
        RouteDetailSharedState.selectedMode = modeLabel
        RouteDetailSharedState.selectedPriority = priorityLabel
    }

    private fun showTransitRouteResult(result: TransitRouteResult) {
        clearRouteOverlays()
        val routePoints = result.steps.toMapPoints()
        if (routePoints.isNotEmpty()) {
            showOriginMarker(routePoints.first().copy(label = getString(R.string.map_marker_origin)))
            showDestinationMarker(routePoints.last().copy(label = getString(R.string.map_marker_destination)))
            showTransitMarkers(routePoints.drop(1).dropLast(1))
            drawRoutePolyline(
                points = routePoints,
                color = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            )
            binding.mapView.controller.apply {
                setZoom(11.0)
                animateTo(pointToGeoPoint(routePoints.first()))
            }
        }

        // Tampilkan sebagai ScoredRoute tunggal di RecyclerView
        val scorer = com.example.naikapa.domain.recommendation.RecommendationScorer()
        val reasonBuilder = com.example.naikapa.domain.recommendation.RecommendationReasonBuilder()
        val candidate = com.example.naikapa.data.model.RouteCandidate.Transit(result)
        val score = scorer.score(candidate, result.sortPreference, emptyList())
        val reason = reasonBuilder.buildReason(candidate, listOf(candidate), result.sortPreference, isMain = true)
        val scoredRoute = com.example.naikapa.data.model.ScoredRoute(
            candidate = candidate,
            score = score,
            reason = reason,
            rankLabel = "Rekomendasi Utama"
        )
        binding.tvPriorityLabel.text = when (result.sortPreference) {
            com.example.naikapa.data.model.SortPreference.FASTEST          -> "TERCEPAT"
            com.example.naikapa.data.model.SortPreference.CHEAPEST         -> "TERHEMAT"
            com.example.naikapa.data.model.SortPreference.MIN_WALKING      -> "MINIM JALAN KAKI"
            com.example.naikapa.data.model.SortPreference.FEWEST_TRANSFERS -> "MINIM TRANSIT"
        }
        routeResultAdapter.submitList(listOf(scoredRoute))
        showRecommendationCard()
        toast(getString(R.string.route_transit_ready))
    }

    private fun showPrivateVehicleRouteResult(
        origin: LocationPoint,
        destination: SearchLocation,
        result: PrivateVehicleRouteResult
    ) {
        clearRouteOverlays()
        val originPoint = MapPoint(
            label = getString(R.string.map_marker_origin),
            latitude = origin.latitude,
            longitude = origin.longitude,
            description = origin.label,
            markerType = MapMarkerType.ORIGIN
        )
        val destinationPoint = MapPoint(
            label = getString(R.string.map_marker_destination),
            latitude = destination.latitude,
            longitude = destination.longitude,
            description = destination.name,
            markerType = MapMarkerType.DESTINATION
        )
        showOriginMarker(originPoint)
        showDestinationMarker(destinationPoint)
        drawRoutePolyline(
            points = result.points,
            color = ContextCompat.getColor(
                requireContext(),
                if (result.mode == PrivateVehicleMode.MOTOR) R.color.colorMotor else R.color.colorMobil
            )
        )
        binding.mapView.controller.apply {
            setZoom(12.0)
            animateTo(pointToGeoPoint(originPoint))
        }

        // Tampilkan sebagai ScoredRoute di RecyclerView
        val scorer = com.example.naikapa.domain.recommendation.RecommendationScorer()
        val reasonBuilder = com.example.naikapa.domain.recommendation.RecommendationReasonBuilder()
        val candidate = com.example.naikapa.data.model.RouteCandidate.PrivateVehicle(result)
        val pref = getSelectedSortPreference()
        val score = scorer.score(candidate, pref, emptyList())
        val reason = reasonBuilder.buildReason(candidate, listOf(candidate), pref, isMain = true)
        val modeLabel = if (result.mode == PrivateVehicleMode.MOTOR) getString(R.string.route_private_motor)
                        else getString(R.string.route_private_car)
        val scoredRoute = com.example.naikapa.data.model.ScoredRoute(
            candidate = candidate,
            score = score,
            reason = reason,
            rankLabel = "Rekomendasi Utama"
        )
        binding.tvPriorityLabel.text = modeLabel.uppercase()
        routeResultAdapter.submitList(listOf(scoredRoute))
        showRecommendationCard()
        toast(getString(R.string.route_private_ready))

        // Simpan riwayat perjalanan
        val priorityLabel = when (getSelectedSortPreference()) {
            SortPreference.FASTEST -> "Tercepat"
            SortPreference.CHEAPEST -> "Terhemat"
            SortPreference.MIN_WALKING -> "Minim Jalan Kaki"
            SortPreference.FEWEST_TRANSFERS -> "Minim Transit"
        }
        saveRouteHistory(origin, destination, scoredRoute, getModeLabel(), priorityLabel)
        RouteDetailSharedState.selectedMode = getModeLabel()
        RouteDetailSharedState.selectedPriority = priorityLabel
    }

    private fun showCombinedRouteResult(
        origin: LocationPoint,
        destination: SearchLocation,
        result: CombinedRouteResult
    ) {
        clearRouteOverlays()
        val originPoint = MapPoint(
            label = getString(R.string.map_marker_origin),
            latitude = origin.latitude,
            longitude = origin.longitude,
            description = origin.label,
            markerType = MapMarkerType.ORIGIN
        )
        val destinationPoint = MapPoint(
            label = getString(R.string.map_marker_destination),
            latitude = destination.latitude,
            longitude = destination.longitude,
            description = destination.name,
            markerType = MapMarkerType.DESTINATION
        )
        val originStopPoint = MapPoint(
            label = result.originStop.stopName,
            latitude = result.originStop.latitude,
            longitude = result.originStop.longitude,
            description = result.originStop.agencyId,
            markerType = MapMarkerType.TRANSIT
        )
        val destinationStopPoint = MapPoint(
            label = result.destinationStop.stopName,
            latitude = result.destinationStop.latitude,
            longitude = result.destinationStop.longitude,
            description = result.destinationStop.agencyId,
            markerType = MapMarkerType.TRANSIT
        )

        showOriginMarker(originPoint)
        showDestinationMarker(destinationPoint)
        showTransitMarkers(listOf(originStopPoint, destinationStopPoint).distinctBy { it.label })
        result.privateVehicleResult?.let { vehicle ->
            drawRoutePolyline(
                points = vehicle.points,
                color = ContextCompat.getColor(
                    requireContext(),
                    if (result.privateVehicleMode == PrivateVehicleMode.MOTOR) R.color.colorMotor else R.color.colorMobil
                )
            )
        }
        result.transitResult?.steps?.toMapPoints()?.let { transitPoints ->
            drawRoutePolyline(
                points = transitPoints,
                color = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            )
        }
        drawRoutePolyline(
            points = listOf(destinationStopPoint, destinationPoint),
            color = ContextCompat.getColor(requireContext(), R.color.colorAccentOrange)
        )
        binding.mapView.controller.apply {
            setZoom(11.0)
            animateTo(pointToGeoPoint(originPoint))
        }

        // Tampilkan sebagai ScoredRoute di RecyclerView
        val scorer = com.example.naikapa.domain.recommendation.RecommendationScorer()
        val reasonBuilder = com.example.naikapa.domain.recommendation.RecommendationReasonBuilder()
        val candidate = com.example.naikapa.data.model.RouteCandidate.Combined(result)
        val pref = getSelectedSortPreference()
        val score = scorer.score(candidate, pref, emptyList())
        val reason = reasonBuilder.buildReason(candidate, listOf(candidate), pref, isMain = true)
        val scoredRoute = com.example.naikapa.data.model.ScoredRoute(
            candidate = candidate,
            score = score,
            reason = reason,
            rankLabel = "Rekomendasi Utama"
        )
        binding.tvPriorityLabel.text = when (pref) {
            com.example.naikapa.data.model.SortPreference.FASTEST          -> "TERCEPAT"
            com.example.naikapa.data.model.SortPreference.CHEAPEST         -> "TERHEMAT"
            com.example.naikapa.data.model.SortPreference.MIN_WALKING      -> "MINIM JALAN KAKI"
            com.example.naikapa.data.model.SortPreference.FEWEST_TRANSFERS -> "MINIM TRANSIT"
        }
        routeResultAdapter.submitList(listOf(scoredRoute))
        showRecommendationCard()
        toast(getString(R.string.route_combined_ready))
    }

    private fun List<RouteStep>.toMapPoints(): List<MapPoint> {
        if (isEmpty()) return emptyList()
        val points = mutableListOf<MapPoint>()
        first().fromStop.let { stop ->
            points.add(
                MapPoint(
                    label = stop.stopName,
                    latitude = stop.lat,
                    longitude = stop.lon,
                    description = stop.agencyId,
                    markerType = MapMarkerType.ORIGIN
                )
            )
        }
        forEach { step ->
            points.add(
                MapPoint(
                    label = step.toStop.stopName,
                    latitude = step.toStop.lat,
                    longitude = step.toStop.lon,
                    description = step.routeShortName,
                    markerType = if (step == last()) MapMarkerType.DESTINATION else MapMarkerType.TRANSIT
                )
            )
        }
        return points
    }

    private fun buildRouteSummary(result: TransitRouteResult): String {
        val firstStep = result.steps.firstOrNull()
        return if (firstStep == null) {
            getString(R.string.route_transit_same_stop)
        } else {
            getString(
                R.string.route_transit_summary_format,
                firstStep.routeShortName,
                result.steps.size,
                formatDistance(result.metrics.totalDistanceMeters)
            )
        }
    }

    private fun getTransitModeForCurrentMode(): TransitMode = when (selectedModeCardId) {
        R.id.modeTJ -> TransitMode.TRANSJAKARTA
        R.id.modeKRL -> TransitMode.KRL
        R.id.modeMRT -> TransitMode.MRT
        R.id.modeLRT -> TransitMode.LRT
        else -> TransitMode.ALL
    }

    private fun getSelectedSortPreference(): SortPreference = when {
        binding.chipTerhemat.isChecked -> SortPreference.CHEAPEST
        binding.chipMinimJalanKaki.isChecked -> SortPreference.MIN_WALKING
        binding.chipMinimTransit.isChecked -> SortPreference.FEWEST_TRANSFERS
        else -> SortPreference.FASTEST
    }

    private fun getSelectedPrivateVehicleMode(): PrivateVehicleMode? = when (selectedModeCardId) {
        R.id.modeMotor -> PrivateVehicleMode.MOTOR
        R.id.modeMobil -> PrivateVehicleMode.CAR
        else -> null
    }

    private fun getCombinedPrivateVehicleMode(): PrivateVehicleMode = PrivateVehicleMode.MOTOR

    private fun getModeLabel(): String = when (selectedModeCardId) {
        R.id.modeTJ -> "TransJakarta"
        R.id.modeKRL -> "KRL"
        R.id.modeMRT -> "MRT"
        R.id.modeLRT -> "LRT"
        R.id.modeMotor -> "Motor"
        R.id.modeMobil -> "Mobil"
        else -> "Campur Semua"
    }

    private fun showDemoRoutePreview() {
        val originPoint = selectedOrigin?.let {
            MapPoint(
                label = getString(R.string.map_marker_origin),
                latitude = it.latitude,
                longitude = it.longitude,
                description = it.label,
                markerType = MapMarkerType.ORIGIN
            )
        } ?: MapPoint(
            label = getString(R.string.map_marker_origin),
            latitude = AppConstants.MAP_DEFAULT_LAT,
            longitude = AppConstants.MAP_DEFAULT_LON,
            description = "Jakarta Pusat",
            markerType = MapMarkerType.ORIGIN
        )
        val transitPoint = MapPoint(
            label = getString(R.string.map_marker_transit),
            latitude = -6.1767,
            longitude = 106.6319,
            description = "Stasiun Tangerang",
            markerType = MapMarkerType.TRANSIT
        )
        val destinationPoint = selectedDestination?.let {
            MapPoint(
                label = getString(R.string.map_marker_destination),
                latitude = it.latitude,
                longitude = it.longitude,
                description = it.name,
                markerType = MapMarkerType.DESTINATION
            )
        } ?: MapPoint(
            label = getString(R.string.map_marker_destination),
            latitude = -6.2386,
            longitude = 106.6284,
            description = binding.tvDestination.text.toString(),
            markerType = MapMarkerType.DESTINATION
        )

        clearRouteOverlays()
        showOriginMarker(originPoint)
        showDestinationMarker(destinationPoint)
        showTransitMarkers(listOf(transitPoint))
        drawRoutePolyline(
            points = listOf(originPoint, transitPoint, destinationPoint),
            color = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        )
        binding.mapView.controller.apply {
            setZoom(10.8)
            animateTo(pointToGeoPoint(transitPoint))
        }
        toast(getString(R.string.map_route_preview_ready))
    }

    private fun showOriginMarker(point: MapPoint) {
        originMarker?.let { binding.mapView.overlays.remove(it) }
        originMarker = createMarker(point).also {
            binding.mapView.overlays.add(it)
        }
        binding.mapView.invalidate()
    }

    private fun showDestinationMarker(point: MapPoint) {
        destinationMarker?.let { binding.mapView.overlays.remove(it) }
        destinationMarker = createMarker(point).also {
            binding.mapView.overlays.add(it)
        }
        binding.mapView.invalidate()
    }

    private fun showTransitMarkers(points: List<MapPoint>) {
        points.forEach { point ->
            addRouteOverlay(createMarker(point))
        }
    }

    private fun drawRoutePolyline(points: List<MapPoint>, color: Int) {
        if (points.size < 2) return
        val polyline = Polyline(binding.mapView).apply {
            outlinePaint.color = color
            outlinePaint.strokeWidth = dpToPx(4f).toFloat()
            outlinePaint.alpha = 220
            setPoints(points.map(::pointToGeoPoint))
        }
        addRouteOverlay(polyline)
    }

    private fun clearRouteOverlays() {
        routeOverlays.forEach { overlay ->
            binding.mapView.overlays.remove(overlay)
        }
        routeOverlays.clear()
        binding.mapView.invalidate()
    }

    private fun addRouteOverlay(overlay: Overlay) {
        routeOverlays.add(overlay)
        binding.mapView.overlays.add(overlay)
        binding.mapView.invalidate()
    }

    private fun createMarker(point: MapPoint): Marker {
        val iconRes = when (point.markerType) {
            MapMarkerType.ORIGIN -> R.drawable.ic_my_location
            MapMarkerType.DESTINATION -> R.drawable.ic_warning
            MapMarkerType.TRANSIT -> R.drawable.ic_train
        }
        val iconColor = when (point.markerType) {
            MapMarkerType.ORIGIN -> R.color.colorPrimary
            MapMarkerType.DESTINATION -> R.color.colorAccentOrange
            MapMarkerType.TRANSIT -> R.color.colorKRL
        }
        val icon = ContextCompat.getDrawable(requireContext(), iconRes)?.mutate()?.apply {
            setTint(ContextCompat.getColor(requireContext(), iconColor))
        }

        return Marker(binding.mapView).apply {
            position = pointToGeoPoint(point)
            title = point.label
            snippet = point.description
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.icon = icon
        }
    }

    private fun pointToGeoPoint(point: MapPoint): GeoPoint {
        return GeoPoint(point.latitude, point.longitude)
    }

    private fun dpToPx(dp: Float): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun setupBottomSheetScrim() {
        val behavior = BottomSheetBehavior.from(binding.cardRecommendation)
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    binding.viewScrim.visibility = View.INVISIBLE
                    binding.viewScrim.alpha = 0f
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        binding.contentContainer.setRenderEffect(null)
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                val coercedOffset = slideOffset.coerceIn(0f, 1f)
                
                // 1. Atur alpha scrim dimming
                binding.viewScrim.apply {
                    if (coercedOffset > 0f) {
                        visibility = View.VISIBLE
                        alpha = coercedOffset * 0.45f
                    } else {
                        visibility = View.INVISIBLE
                        alpha = 0f
                    }
                }
                
                // 2. Efek blur untuk API 31+ (Android 12+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val maxBlurRadius = 16f
                    val currentBlurRadius = coercedOffset * maxBlurRadius
                    
                    if (currentBlurRadius > 0.1f) {
                        val blurEffect = RenderEffect.createBlurEffect(
                            currentBlurRadius,
                            currentBlurRadius,
                            Shader.TileMode.CLAMP
                        )
                        binding.contentContainer.setRenderEffect(blurEffect)
                    } else {
                        binding.contentContainer.setRenderEffect(null)
                    }
                }
            }
        })
    }

    private fun startLocationFlow() {
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        fetchCurrentLocation()
    }

    private fun hasLocationPermission(): Boolean {
        val context = requireContext()
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    private fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationServiceEnabled(): Boolean {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation() {
        if (!isLocationServiceEnabled()) {
            toast(getString(R.string.location_service_disabled))
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        toast(getString(R.string.location_fetching))
        binding.btnLocation.isEnabled = false

        val priority = if (hasFineLocationPermission()) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(priority, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                if (_binding == null) return@addOnSuccessListener
                binding.btnLocation.isEnabled = true
                if (location != null) {
                    handleLocationResult(location)
                } else {
                    toast(getString(R.string.location_failed))
                }
            }
            .addOnFailureListener {
                if (_binding == null) return@addOnFailureListener
                binding.btnLocation.isEnabled = true
                toast(getString(R.string.location_error))
            }
    }

    private fun handleLocationResult(location: Location) {
        val origin = LocationPoint(
            label = getString(R.string.search_dari_val),
            latitude = location.latitude,
            longitude = location.longitude,
            isFromGps = true
        )
        selectedOrigin = origin
        selectedOriginStop = null

        binding.tvOrigin.text = origin.label
        binding.tvOriginSub.text = formatCoordinates(origin.latitude, origin.longitude)
        showOriginMarker(
            MapPoint(
                label = getString(R.string.map_marker_origin),
                latitude = origin.latitude,
                longitude = origin.longitude,
                description = origin.label,
                markerType = MapMarkerType.ORIGIN
            )
        )
        binding.mapView.controller.apply {
            setZoom(AppConstants.MAP_LOCATION_ZOOM)
            animateTo(GeoPoint(origin.latitude, origin.longitude))
        }
        toast(getString(R.string.location_success))
    }

    private fun formatCoordinates(latitude: Double, longitude: Double): String {
        return String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
    }

    private fun formatDuration(seconds: Int): String {
        val minutes = (seconds + 59) / 60
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return if (hours > 0) {
                    getString(R.string.route_duration_hour_minute_format, hours, remainingMinutes)
        } else {
            getString(R.string.route_duration_minute_format, minutes)
        }
    }

    private fun formatRupiah(value: Int): String {
        return getString(R.string.route_cost_rupiah_format, String.format(Locale.US, "%,d", value).replace(",", "."))
    }

    private fun formatDistance(distanceMeters: Double): String {
        return if (distanceMeters >= 1000.0) {
            getString(R.string.search_gtfs_distance_km, distanceMeters / 1000.0)
        } else {
            getString(R.string.search_gtfs_distance_m, distanceMeters.toInt())
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        // Cek apakah ada replay request dari RiwayatFragment
        if (HistoryReplayRequest.hasPending()) {
            applyHistoryReplayRequest()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    /**
     * Terapkan replay request dari RiwayatFragment: isi origin/destination/mode/priority
     * lalu trigger pencarian rute secara otomatis.
     */
    private fun applyHistoryReplayRequest() {
        val req = HistoryReplayRequest
        val dest = req.pendingDestination ?: return

        // Isi destination
        selectedDestination = dest
        binding.tvDestination.text = dest.name
        binding.tvDestinationSub.text = dest.address ?: formatCoordinates(dest.latitude, dest.longitude)
        showDestinationMarker(
            MapPoint(
                label = getString(R.string.map_marker_destination),
                latitude = dest.latitude,
                longitude = dest.longitude,
                description = dest.name,
                markerType = MapMarkerType.DESTINATION
            )
        )

        // Isi origin jika ada
        req.pendingOrigin?.let { origin ->
            selectedOrigin = origin
            selectedOriginStop = null
            binding.tvOrigin.text = origin.label
            binding.tvOriginSub.text = formatCoordinates(origin.latitude, origin.longitude)
            showOriginMarker(
                MapPoint(
                    label = getString(R.string.map_marker_origin),
                    latitude = origin.latitude,
                    longitude = origin.longitude,
                    description = origin.label,
                    markerType = MapMarkerType.ORIGIN
                )
            )
        }

        // Terapkan mode jika ada (cocokkan ke card)
        req.pendingMode?.let { mode ->
            val targetCard = when {
                mode.contains("Motor", ignoreCase = true) -> binding.modeMotor
                mode.contains("Mobil", ignoreCase = true) -> binding.modeMobil
                mode.contains("TransJakarta", ignoreCase = true) || mode.contains("TJ", ignoreCase = true) -> binding.modeTJ
                mode.contains("KRL", ignoreCase = true) -> binding.modeKRL
                mode.contains("MRT", ignoreCase = true) -> binding.modeMRT
                mode.contains("LRT", ignoreCase = true) -> binding.modeLRT
                else -> binding.modeCampur
            }
            targetCard.isChecked = true
        }

        // Terapkan priority jika ada
        req.pendingPriority?.let { priority ->
            when {
                priority.contains("Hemat", ignoreCase = true) -> binding.chipTerhemat.isChecked = true
                priority.contains("Jalan", ignoreCase = true) -> binding.chipMinimJalanKaki.isChecked = true
                priority.contains("Transit", ignoreCase = true) -> binding.chipMinimTransit.isChecked = true
                else -> binding.chipTercepat.isChecked = true
            }
        }

        req.clear()

        // Trigger pencarian jika origin sudah tersedia
        if (selectedOrigin != null) {
            handleFindRouteClick()
        }
    }

    /**
     * Simpan riwayat perjalanan ke tabel route_history di background thread.
     */
    private fun saveRouteHistory(
        origin: LocationPoint,
        destination: SearchLocation,
        scoredRoute: ScoredRoute,
        modeLabel: String,
        priorityLabel: String
    ) {
        val userId = sessionManager.getUserId()
        if (userId <= 0) return
        val metrics = scoredRoute.candidate.metrics
        homeScope.launch(Dispatchers.IO) {
            historyDao.insertRouteHistory(
                RouteHistory(
                    idUser = userId,
                    originName = origin.label,
                    destinationName = destination.name,
                    mode = modeLabel,
                    priority = priorityLabel,
                    recommendationSummary = scoredRoute.reason.take(120),
                    score = scoredRoute.score,
                    estimatedTime = metrics.totalDurationSeconds,
                    estimatedCost = metrics.estimatedTotalCost,
                    estimatedBbm = metrics.estimatedBbm,
                    walkingDistance = metrics.walkingDistanceMeters,
                    transitCount = metrics.transitCount
                )
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        destinationSearchJob?.cancel()
        homeScope.cancel()
        if (::dbHelper.isInitialized) dbHelper.close()
        _binding = null
    }

    private fun restoreSearchFieldsState() {
        val origin = selectedOrigin
        val destination = selectedDestination

        if (origin != null) {
            binding.tvOrigin.text = origin.label
            binding.tvOriginSub.text = formatCoordinates(origin.latitude, origin.longitude)
            val originPoint = MapPoint(
                label = getString(R.string.map_marker_origin),
                latitude = origin.latitude,
                longitude = origin.longitude,
                description = origin.label,
                markerType = MapMarkerType.ORIGIN
            )
            showOriginMarker(originPoint)
        }

        if (destination != null) {
            binding.tvDestination.text = destination.name
            binding.tvDestinationSub.text = destination.address ?: formatCoordinates(destination.latitude, destination.longitude)
            val destinationPoint = MapPoint(
                label = getString(R.string.map_marker_destination),
                latitude = destination.latitude,
                longitude = destination.longitude,
                description = destination.name,
                markerType = MapMarkerType.DESTINATION
            )
            showDestinationMarker(destinationPoint)
        }

        if (origin != null || destination != null) {
            binding.mapView.controller.apply {
                setZoom(AppConstants.MAP_LOCATION_ZOOM)
                if (origin != null) {
                    setCenter(GeoPoint(origin.latitude, origin.longitude))
                } else if (destination != null) {
                    setCenter(GeoPoint(destination.latitude, destination.longitude))
                }
            }
        }
    }

    companion object {
        private var savedOrigin: LocationPoint? = null
        private var savedOriginStop: SearchLocation? = null
        private var savedDestination: SearchLocation? = null
    }
}
