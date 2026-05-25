package com.example.naikapa.presentation.route_detail

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.naikapa.R
import com.example.naikapa.common.SessionManager
import com.example.naikapa.common.toast
import com.example.naikapa.common.applyStatusBarTopMarginTo
import com.example.naikapa.data.local.NaikApaDatabaseHelper
import com.example.naikapa.data.local.SavedTripDao
import com.example.naikapa.data.model.*
import com.example.naikapa.data.repository.GtfsStopSearchRepository
import com.example.naikapa.databinding.FragmentRouteDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import java.util.Locale

class RouteDetailFragment : Fragment() {

    private var _binding: FragmentRouteDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var dbHelper: NaikApaDatabaseHelper
    private lateinit var savedTripDao: SavedTripDao
    private var isFavoriteSaved = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRouteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        dbHelper = NaikApaDatabaseHelper(requireContext())
        savedTripDao = SavedTripDao(dbHelper)

        val selectedRoute = RouteDetailSharedState.selectedRoute
        if (selectedRoute == null) {
            toast("Rute tidak ditemukan")
            findNavController().popBackStack()
            return
        }

        setupToolbar()
        binding.root.applyStatusBarTopMarginTo(binding.cardToolbar, dpToPx(8f))
        bindSummaryCard(selectedRoute)
        bindMetrics(selectedRoute.candidate.metrics)
        setupDisruptionWarning(selectedRoute)
        setupTimeline(selectedRoute.candidate)
        setupMap(selectedRoute.candidate)
        setupFavoriteButton(selectedRoute)
        setupReportButton(selectedRoute)
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun bindSummaryCard(route: ScoredRoute) {
        binding.tvSummaryTitle.text = route.candidate.candidateLabel
        binding.tvMatchScore.text = "${route.score}% Cocok"
        binding.tvSummaryReason.text = route.reason
    }

    private fun bindMetrics(metrics: RouteMetrics) {
        binding.tvDurationValue.text = formatDuration(metrics.totalDurationSeconds)
        binding.tvFareValue.text = formatRupiah(metrics.estimatedTotalCost)
        binding.tvWalkingValue.text = formatDistance(metrics.walkingDistanceMeters)
        binding.tvTransitValue.text = "${metrics.transitCount}x transit"
    }

    private fun setupDisruptionWarning(route: ScoredRoute) {
        if (route.hasDisruptionWarning && !route.disruptionWarningText.isNullOrBlank()) {
            binding.tvDisruptionText.text = route.disruptionWarningText
            binding.layoutDisruptionWarning.visibility = View.VISIBLE
        } else {
            binding.layoutDisruptionWarning.visibility = View.GONE
        }
    }

    private fun setupTimeline(candidate: RouteCandidate) {
        val detailSteps = buildDetailSteps(candidate, requireContext())
        val adapter = RouteStepAdapter(detailSteps)
        binding.rvTimelineSteps.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupMap(candidate: RouteCandidate) {
        binding.detailMapView.apply {
            setMultiTouchControls(true)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            setTileSource(
                XYTileSource(
                    "CartoDB Positron", 1, 19, 256, ".png",
                    arrayOf(
                        "https://a.basemaps.cartocdn.com/light_all/",
                        "https://b.basemaps.cartocdn.com/light_all/",
                        "https://c.basemaps.cartocdn.com/light_all/",
                        "https://d.basemaps.cartocdn.com/light_all/"
                    )
                )
            )
            // Cegah NestedScrollView mengambil alih touch saat user berinteraksi dengan map
            setOnTouchListener { v, _ ->
                v.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
        }

        val overlays = mutableListOf<Overlay>()
        val context = requireContext()

        val origin = RouteDetailSharedState.origin
        val destination = RouteDetailSharedState.destination

        val originPoint = origin?.let {
            MapPoint(
                label = getString(R.string.map_marker_origin),
                latitude = it.latitude,
                longitude = it.longitude,
                description = it.label,
                markerType = MapMarkerType.ORIGIN
            )
        }
        val destinationPoint = destination?.let {
            MapPoint(
                label = getString(R.string.map_marker_destination),
                latitude = it.latitude,
                longitude = it.longitude,
                description = it.name,
                markerType = MapMarkerType.DESTINATION
            )
        }

        if (originPoint != null) {
            overlays.add(createMarker(binding.detailMapView, originPoint, context))
        }
        if (destinationPoint != null) {
            overlays.add(createMarker(binding.detailMapView, destinationPoint, context))
        }

        var centerGeoPoint: GeoPoint? = originPoint?.let { GeoPoint(it.latitude, it.longitude) }

        when (candidate) {
            is RouteCandidate.Transit -> {
                val routePoints = candidate.result.steps.toMapPoints()
                if (routePoints.size > 1) {
                    val transitPoints = routePoints.drop(1).dropLast(1)
                    transitPoints.forEach { pt ->
                        overlays.add(createMarker(binding.detailMapView, pt, context))
                    }
                    overlays.add(createPolyline(binding.detailMapView, routePoints, ContextCompat.getColor(context, R.color.colorPrimary)))
                    centerGeoPoint = GeoPoint(routePoints.first().latitude, routePoints.first().longitude)
                }
            }
            is RouteCandidate.PrivateVehicle -> {
                val color = if (candidate.result.mode == PrivateVehicleMode.MOTOR)
                    ContextCompat.getColor(context, R.color.colorMotor)
                else ContextCompat.getColor(context, R.color.colorMobil)
                overlays.add(createPolyline(binding.detailMapView, candidate.result.points, color))
                if (candidate.result.points.isNotEmpty()) {
                    centerGeoPoint = GeoPoint(candidate.result.points.first().latitude, candidate.result.points.first().longitude)
                }
            }
            is RouteCandidate.Combined -> {
                val vehicleResult = candidate.result.privateVehicleResult
                val transitResult = candidate.result.transitResult

                if (vehicleResult != null) {
                    val color = if (vehicleResult.mode == PrivateVehicleMode.MOTOR)
                        ContextCompat.getColor(context, R.color.colorMotor)
                    else ContextCompat.getColor(context, R.color.colorMobil)
                    overlays.add(createPolyline(binding.detailMapView, vehicleResult.points, color))
                } else {
                    val firstSegment = candidate.result.segments.firstOrNull { it.type == CombinedRouteSegmentType.WALKING }
                    if (firstSegment != null && firstSegment.points.isNotEmpty()) {
                        overlays.add(createPolyline(binding.detailMapView, firstSegment.points, ContextCompat.getColor(context, R.color.colorWalking)))
                    }
                }
                if (transitResult != null) {
                    val transitPoints = transitResult.steps.toMapPoints()
                    if (transitPoints.size > 1) {
                        val transitMarkerPoints = transitPoints.drop(1).dropLast(1)
                        transitMarkerPoints.forEach { pt ->
                            overlays.add(createMarker(binding.detailMapView, pt, context))
                        }
                        overlays.add(createPolyline(binding.detailMapView, transitPoints, ContextCompat.getColor(context, R.color.colorPrimary)))
                    }
                }

                val originStopPoint = MapPoint(
                    label = candidate.result.originStop.stopName,
                    latitude = candidate.result.originStop.latitude,
                    longitude = candidate.result.originStop.longitude,
                    description = GtfsStopSearchRepository.agencyIdToLabel(candidate.result.originStop.agencyId),
                    markerType = MapMarkerType.TRANSIT
                )
                val destinationStopPoint = MapPoint(
                    label = candidate.result.destinationStop.stopName,
                    latitude = candidate.result.destinationStop.latitude,
                    longitude = candidate.result.destinationStop.longitude,
                    description = GtfsStopSearchRepository.agencyIdToLabel(candidate.result.destinationStop.agencyId),
                    markerType = MapMarkerType.TRANSIT
                )
                overlays.add(createMarker(binding.detailMapView, originStopPoint, context))
                overlays.add(createMarker(binding.detailMapView, destinationStopPoint, context))

                if (destinationPoint != null) {
                    overlays.add(createPolyline(binding.detailMapView, listOf(destinationStopPoint, destinationPoint), ContextCompat.getColor(context, R.color.colorAccentOrange)))
                }
            }
        }

        overlays.forEach { binding.detailMapView.overlays.add(it) }

        binding.detailMapView.controller.apply {
            setZoom(11.8)
            if (centerGeoPoint != null) {
                setCenter(centerGeoPoint)
            }
        }
        binding.detailMapView.invalidate()
    }

    private fun setupReportButton(route: ScoredRoute) {
        binding.btnReportDisruption.setOnClickListener {
            // Ambil stopId dan routeId pertama dari rute transit jika tersedia
            val prefillStop: String? = when (val candidate = route.candidate) {
                is RouteCandidate.Transit -> candidate.result.steps.firstOrNull()?.fromStop?.stopId
                is RouteCandidate.Combined -> candidate.result.originStop.stopId
                else -> null
            }
            val prefillRoute: String? = when (val candidate = route.candidate) {
                is RouteCandidate.Transit -> candidate.result.steps.firstOrNull()?.routeId
                is RouteCandidate.Combined -> candidate.result.transitResult?.steps?.firstOrNull()?.routeId
                else -> null
            }
            findNavController().navigate(
                R.id.addEditDisruptionReportFragment,
                android.os.Bundle().apply {
                    putLong(com.example.naikapa.presentation.report.AddEditDisruptionReportFragment.ARG_REPORT_ID, 0L)
                    prefillStop?.let  { putString(com.example.naikapa.presentation.report.AddEditDisruptionReportFragment.ARG_STOP_ID, it) }
                    prefillRoute?.let { putString(com.example.naikapa.presentation.report.AddEditDisruptionReportFragment.ARG_ROUTE_ID, it) }
                }
            )
        }
    }

    private fun setupFavoriteButton(route: ScoredRoute) {
        // Reset state saat fragment dibuka ulang
        isFavoriteSaved = false
        binding.btnSaveFavorite.isEnabled = true
        binding.btnSaveFavorite.text = getString(R.string.route_detail_save_favorite)

        binding.btnSaveFavorite.setOnClickListener {
            if (isFavoriteSaved) {
                toast(getString(R.string.history_favorit_already_saved))
                return@setOnClickListener
            }

            val userId = sessionManager.getUserId()
            if (userId <= 0) {
                toast("Silakan login terlebih dahulu untuk menyimpan favorit.")
                return@setOnClickListener
            }

            val origin = RouteDetailSharedState.origin ?: return@setOnClickListener
            val destination = RouteDetailSharedState.destination ?: return@setOnClickListener

            // Gunakan mode/priority dari SharedState jika tersedia, fallback ke candidateLabel
            val mode = RouteDetailSharedState.selectedMode ?: route.candidate.candidateLabel
            val priority = RouteDetailSharedState.selectedPriority ?: "Terpilih"

            val savedTrip = SavedTrip(
                idUser = userId,
                namaPerjalanan = "${origin.label} ke ${destination.name}",
                originName = origin.label,
                originLat = origin.latitude,
                originLon = origin.longitude,
                destinationName = destination.name,
                destinationLat = destination.latitude,
                destinationLon = destination.longitude,
                mode = mode,
                priority = priority
            )

            binding.btnSaveFavorite.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val result = savedTripDao.insert(savedTrip)
                withContext(Dispatchers.Main) {
                    if (result > 0) {
                        isFavoriteSaved = true
                        toast(getString(R.string.history_favorit_saved))
                        binding.btnSaveFavorite.text = getString(R.string.history_favorit_already_saved)
                        binding.btnSaveFavorite.setIconResource(R.drawable.ic_heart)
                    } else {
                        toast(getString(R.string.history_favorit_save_failed))
                        binding.btnSaveFavorite.isEnabled = true
                    }
                }
            }
        }
    }

    private fun buildDetailSteps(candidate: RouteCandidate, context: Context): List<DetailStep> {
        val steps = mutableListOf<DetailStep>()
        when (candidate) {
            is RouteCandidate.Transit -> {
                candidate.result.steps.forEach { step ->
                    if (step.type == TransitEdgeType.TRANSIT) {
                        val agencyLabel = when (step.agencyId) {
                            "Tije"  -> "TransJakarta"
                            "KAIC"  -> "KRL"
                            "MRTJ"  -> "MRT"
                            "LRTJ"  -> "LRT"
                            "LRTJB" -> "LRT Jabodebek"
                            else    -> step.agencyId
                        }
                        val routeLabel = if (!step.routeShortName.isNullOrBlank()) {
                            "Naik $agencyLabel (${step.routeShortName})"
                        } else {
                            "Naik $agencyLabel"
                        }
                        steps.add(
                            DetailStep(
                                title = routeLabel,
                                description = "Naik dari ${step.fromStop.stopName}\nTurun di ${step.toStop.stopName} (${step.stopCount} perhentian)",
                                durationSeconds = step.durationSeconds,
                                distanceMeters = step.distanceMeters,
                                iconResId = getIconForAgency(step.agencyId),
                                colorInt = parseRouteColor(step.routeColor, context, step.agencyId)
                            )
                        )
                    } else {
                        steps.add(
                            DetailStep(
                                title = "Jalan Kaki",
                                description = "Dari ${step.fromStop.stopName} ke ${step.toStop.stopName}",
                                durationSeconds = step.durationSeconds,
                                distanceMeters = step.distanceMeters,
                                iconResId = R.drawable.ic_walk,
                                colorInt = ContextCompat.getColor(context, R.color.colorWalking)
                            )
                        )
                    }
                }
            }
            is RouteCandidate.PrivateVehicle -> {
                val modeName = if (candidate.result.mode == PrivateVehicleMode.MOTOR) "Motor" else "Mobil"
                val iconRes = if (candidate.result.mode == PrivateVehicleMode.MOTOR) R.drawable.ic_motorcycle else R.drawable.ic_car
                val color = if (candidate.result.mode == PrivateVehicleMode.MOTOR) R.color.colorMotor else R.color.colorMobil
                steps.add(
                    DetailStep(
                        title = "Perjalanan dengan $modeName",
                        description = "Rute langsung ke lokasi tujuan",
                        durationSeconds = candidate.result.travelTimeSeconds,
                        distanceMeters = candidate.result.distanceMeters.toDouble(),
                        iconResId = iconRes,
                        colorInt = ContextCompat.getColor(context, color)
                    )
                )
            }
            is RouteCandidate.Combined -> {
                candidate.result.segments.forEach { segment ->
                    when (segment.type) {
                        CombinedRouteSegmentType.PRIVATE_VEHICLE -> {
                            val modeName = if (candidate.result.privateVehicleMode == PrivateVehicleMode.MOTOR) "Motor" else "Mobil"
                            val iconRes = if (candidate.result.privateVehicleMode == PrivateVehicleMode.MOTOR) R.drawable.ic_motorcycle else R.drawable.ic_car
                            val color = if (candidate.result.privateVehicleMode == PrivateVehicleMode.MOTOR) R.color.colorMotor else R.color.colorMobil
                            steps.add(
                                DetailStep(
                                    title = "Perjalanan dengan $modeName",
                                    description = segment.title,
                                    durationSeconds = segment.durationSeconds,
                                    distanceMeters = segment.distanceMeters,
                                    iconResId = iconRes,
                                    colorInt = ContextCompat.getColor(context, color)
                                )
                            )
                        }
                        CombinedRouteSegmentType.TRANSIT -> {
                            segment.transitResult?.steps?.forEach { step ->
                                if (step.type == TransitEdgeType.TRANSIT) {
                                    val agencyLabel = when (step.agencyId) {
                                        "Tije"  -> "TransJakarta"
                                        "KAIC"  -> "KRL"
                                        "MRTJ"  -> "MRT"
                                        "LRTJ"  -> "LRT"
                                        "LRTJB" -> "LRT Jabodebek"
                                        else    -> step.agencyId
                                    }
                                    val routeLabel = if (!step.routeShortName.isNullOrBlank()) {
                                        "Naik $agencyLabel (${step.routeShortName})"
                                    } else {
                                        "Naik $agencyLabel"
                                    }
                                    steps.add(
                                        DetailStep(
                                            title = routeLabel,
                                            description = "Naik dari ${step.fromStop.stopName}\nTurun di ${step.toStop.stopName} (${step.stopCount} perhentian)",
                                            durationSeconds = step.durationSeconds,
                                            distanceMeters = step.distanceMeters,
                                            iconResId = getIconForAgency(step.agencyId),
                                            colorInt = parseRouteColor(step.routeColor, context, step.agencyId)
                                        )
                                    )
                                } else {
                                    steps.add(
                                        DetailStep(
                                            title = "Jalan Kaki",
                                            description = "Dari ${step.fromStop.stopName} ke ${step.toStop.stopName}",
                                            durationSeconds = step.durationSeconds,
                                            distanceMeters = step.distanceMeters,
                                            iconResId = R.drawable.ic_walk,
                                            colorInt = ContextCompat.getColor(context, R.color.colorWalking)
                                        )
                                    )
                                }
                            }
                        }
                        CombinedRouteSegmentType.WALKING -> {
                            steps.add(
                                DetailStep(
                                    title = "Jalan Kaki",
                                    description = segment.title,
                                    durationSeconds = segment.durationSeconds,
                                    distanceMeters = segment.distanceMeters,
                                    iconResId = R.drawable.ic_walk,
                                    colorInt = ContextCompat.getColor(context, R.color.colorWalking)
                                )
                            )
                        }
                    }
                }
            }
        }
        return steps
    }

    private fun parseRouteColor(colorStr: String?, context: Context, agencyId: String): Int {
        if (!colorStr.isNullOrBlank()) {
            try {
                val hexColor = if (colorStr.startsWith("#")) colorStr else "#$colorStr"
                return android.graphics.Color.parseColor(hexColor)
            } catch (e: Exception) {
                // ignore
            }
        }
        return getColorForAgency(agencyId, context)
    }

    private fun getIconForAgency(agencyId: String): Int = when (agencyId) {
        "Tije" -> R.drawable.ic_bus
        "KAIC", "MRTJ", "LRTJ", "LRTJB" -> R.drawable.ic_train
        else -> R.drawable.ic_train
    }

    private fun getColorForAgency(agencyId: String, context: Context): Int = when (agencyId) {
        "Tije"  -> ContextCompat.getColor(context, R.color.colorTransjakarta)
        "KAIC"  -> ContextCompat.getColor(context, R.color.colorKRL)
        "MRTJ"  -> ContextCompat.getColor(context, R.color.colorMRT)
        "LRTJ"  -> ContextCompat.getColor(context, R.color.colorLRT)
        "LRTJB" -> ContextCompat.getColor(context, R.color.colorLRT)
        else    -> ContextCompat.getColor(context, R.color.colorPrimary)
    }

    private fun createMarker(mapView: MapView, point: MapPoint, context: Context): Marker {
        val iconRes = when (point.markerType) {
            MapMarkerType.ORIGIN -> R.drawable.ic_my_location
            MapMarkerType.DESTINATION -> R.drawable.ic_destination
            MapMarkerType.TRANSIT -> R.drawable.ic_train
        }
        val iconColor = when (point.markerType) {
            MapMarkerType.ORIGIN -> R.color.colorPrimary
            MapMarkerType.DESTINATION -> R.color.colorAccentOrange
            MapMarkerType.TRANSIT -> R.color.colorKRL
        }
        val icon = ContextCompat.getDrawable(context, iconRes)?.mutate()?.apply {
            setTint(ContextCompat.getColor(context, iconColor))
        }

        return Marker(mapView).apply {
            position = GeoPoint(point.latitude, point.longitude)
            title = point.label
            snippet = point.description
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.icon = icon
        }
    }

    private fun createPolyline(mapView: MapView, points: List<MapPoint>, color: Int): Polyline {
        return Polyline(mapView).apply {
            outlinePaint.color = color
            outlinePaint.strokeWidth = 10f
            outlinePaint.alpha = 220
            setPoints(points.map { GeoPoint(it.latitude, it.longitude) })
        }
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

    private fun formatDuration(seconds: Int): String {
        val minutes = (seconds + 59) / 60
        return if (minutes >= 60) {
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            if (remainingMinutes > 0) {
                "$hours j $remainingMinutes mnt"
            } else {
                "$hours j"
            }
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

    private fun dpToPx(dp: Float): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onResume() {
        super.onResume()
        binding.detailMapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.detailMapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::dbHelper.isInitialized) dbHelper.close()
        _binding = null
    }
}
