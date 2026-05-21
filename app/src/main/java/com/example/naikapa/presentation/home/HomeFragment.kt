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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.naikapa.BuildConfig
import com.example.naikapa.R
import com.example.naikapa.common.AppConstants
import com.example.naikapa.common.SessionManager
import com.example.naikapa.common.toast
import com.example.naikapa.data.local.HistoryDao
import com.example.naikapa.data.local.NaikApaDatabaseHelper
import com.example.naikapa.data.model.LocationPoint
import com.example.naikapa.data.model.MapMarkerType
import com.example.naikapa.data.model.MapPoint
import com.example.naikapa.data.model.MapStyle
import com.example.naikapa.data.model.SearchHistory
import com.example.naikapa.data.model.SearchLocation
import com.example.naikapa.data.remote.RemoteClient
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var dbHelper: NaikApaDatabaseHelper
    private lateinit var historyDao: HistoryDao
    private val searchRepository = TomTomSearchRepository(RemoteClient.tomTomSearchApi)
    private lateinit var homeScope: CoroutineScope
    private var selectedOrigin: LocationPoint? = null
    private var selectedDestination: SearchLocation? = null
    private var destinationSearchJob: Job? = null
    private var currentMapStyle = MapStyle.POSITRON
    private val routeOverlays = mutableListOf<Overlay>()
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null

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
        homeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        // Mengubah sapaan di header dengan nama user
        val userName = sessionManager.getUserName() ?: "User NaikApa"
        binding.tvWelcomeUser.text = "Halo, $userName!"
        binding.tvWelcomeUser.visibility = View.VISIBLE

        initMap()
        setupTransitModes()
        setupDestinationSearch()
        setupRouteActions()
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

    private fun setupDestinationSearch() {
        binding.etDestinationSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                scheduleDestinationSearch(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun scheduleDestinationSearch(query: String) {
        destinationSearchJob?.cancel()
        val trimmedQuery = query.trim()
        if (trimmedQuery.length < AppConstants.TOMTOM_MIN_QUERY_LENGTH) {
            hideSearchResults()
            return
        }
        if (BuildConfig.TOMTOM_API_KEY == AppConstants.TOMTOM_API_KEY_PLACEHOLDER) {
            showSearchStatus(getString(R.string.search_destination_api_key_missing), isError = true)
            return
        }

        destinationSearchJob = homeScope.launch {
            delay(AppConstants.TOMTOM_SEARCH_DEBOUNCE_MS)
            performDestinationSearch(trimmedQuery)
        }
    }

    private suspend fun performDestinationSearch(query: String) {
        showSearchStatus(getString(R.string.search_destination_loading), isError = false)
        val latitudeBias = selectedOrigin?.latitude ?: AppConstants.MAP_DEFAULT_LAT
        val longitudeBias = selectedOrigin?.longitude ?: AppConstants.MAP_DEFAULT_LON
        val result = withContext(Dispatchers.IO) {
            searchRepository.search(
                query = query,
                apiKey = BuildConfig.TOMTOM_API_KEY,
                latitudeBias = latitudeBias,
                longitudeBias = longitudeBias
            )
        }

        result
            .onSuccess { locations ->
                if (locations.isEmpty()) {
                    showSearchStatus(getString(R.string.search_destination_empty), isError = false)
                } else {
                    renderSearchResults(query, locations)
                }
            }
            .onFailure {
                showSearchStatus(getString(R.string.search_destination_error), isError = true)
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
        val card = MaterialCardView(requireContext()).apply {
            radius = dpToPx(10f).toFloat()
            cardElevation = 0f
            strokeWidth = dpToPx(1f)
            strokeColor = ContextCompat.getColor(requireContext(), R.color.colorCardOutline)
            setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            setOnClickListener {
                selectDestination(query, location)
            }
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
        card.addView(content)
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dpToPx(8f)
        }
        return card
    }

    private fun selectDestination(query: String, location: SearchLocation) {
        selectedDestination = location
        destinationSearchJob?.cancel()
        binding.tvDestination.text = location.name
        binding.tvDestinationSub.text = location.address ?: formatCoordinates(location.latitude, location.longitude)
        binding.etDestinationSearch.setText("")
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
            startLocationFlow()
        }

        binding.btnMapStyle.setOnClickListener {
            toggleMapStyle()
        }

        // Tombol Cari Rute
        binding.btnTemukanRute.setOnClickListener {
            val origin = binding.tvOrigin.text.toString()
            val destination = binding.tvDestination.text.toString()
            toast("Mencari rute dari $origin ke $destination...")
            showDemoRoutePreview()
            
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
        destinationSearchJob?.cancel()
        homeScope.cancel()
        if (::dbHelper.isInitialized) dbHelper.close()
        _binding = null
    }
}
