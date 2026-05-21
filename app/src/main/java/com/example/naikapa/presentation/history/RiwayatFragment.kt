package com.example.naikapa.presentation.history

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.naikapa.R
import com.example.naikapa.common.SessionManager
import com.example.naikapa.common.toast
import com.example.naikapa.data.local.HistoryDao
import com.example.naikapa.data.local.NaikApaDatabaseHelper
import com.example.naikapa.data.local.SavedTripDao
import com.example.naikapa.data.model.LocationPoint
import com.example.naikapa.data.model.SavedTrip
import com.example.naikapa.data.model.SearchHistory
import com.example.naikapa.data.model.SearchLocation
import com.example.naikapa.databinding.FragmentRiwayatBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RiwayatFragment : Fragment() {

    private var _binding: FragmentRiwayatBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var dbHelper: NaikApaDatabaseHelper
    private lateinit var savedTripDao: SavedTripDao
    private lateinit var historyDao: HistoryDao

    private lateinit var savedTripAdapter: SavedTripAdapter
    private lateinit var searchHistoryAdapter: SearchHistoryAdapter
    private lateinit var routeHistoryAdapter: RouteHistoryAdapter

    private enum class ActiveTab { FAVORIT, PENCARIAN, PERJALANAN }
    private var activeTab = ActiveTab.FAVORIT

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiwayatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        dbHelper = NaikApaDatabaseHelper(requireContext())
        savedTripDao = SavedTripDao(dbHelper)
        historyDao = HistoryDao(dbHelper)

        setupAdapters()
        setupTabs()
        selectTab(ActiveTab.FAVORIT)
    }

    override fun onResume() {
        super.onResume()
        loadCurrentTab()
    }

    private fun setupAdapters() {
        savedTripAdapter = SavedTripAdapter(
            onReplay = { trip -> replayFavorit(trip) },
            onEdit = { trip -> showEditFavoritDialog(trip) },
            onDelete = { trip -> confirmDeleteFavorit(trip) }
        )
        searchHistoryAdapter = SearchHistoryAdapter(
            onUseAsDestination = { history -> useSearchHistoryAsDestination(history) },
            onDelete = { history -> deleteSearchHistory(history) }
        )
        routeHistoryAdapter = RouteHistoryAdapter(
            onDelete = { history -> deleteRouteHistory(history) }
        )

        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupTabs() {
        binding.tabFavorit.setOnClickListener { selectTab(ActiveTab.FAVORIT) }
        binding.tabRiwayatPencarian.setOnClickListener { selectTab(ActiveTab.PENCARIAN) }
        binding.tabRiwayatPerjalanan.setOnClickListener { selectTab(ActiveTab.PERJALANAN) }
        binding.btnClearAll.setOnClickListener { confirmClearAll() }
    }

    private fun selectTab(tab: ActiveTab) {
        activeTab = tab
        updateTabStyles()
        loadCurrentTab()
    }

    private fun updateTabStyles() {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.colorTextSecondary)
        val activeBg = ContextCompat.getColor(requireContext(), R.color.colorPrimaryLight)
        val inactiveBg = android.graphics.Color.TRANSPARENT

        listOf(
            binding.tabFavorit to (activeTab == ActiveTab.FAVORIT),
            binding.tabRiwayatPencarian to (activeTab == ActiveTab.PENCARIAN),
            binding.tabRiwayatPerjalanan to (activeTab == ActiveTab.PERJALANAN)
        ).forEach { (tab, isActive) ->
            tab.setTextColor(if (isActive) activeColor else inactiveColor)
            tab.setBackgroundColor(if (isActive) activeBg else inactiveBg)
            tab.background = if (isActive) {
                android.graphics.drawable.GradientDrawable().apply {
                    setColor(activeBg)
                    cornerRadius = resources.displayMetrics.density * 8
                }
            } else null
        }
    }

    private fun loadCurrentTab() {
        val userId = sessionManager.getUserId()
        if (userId <= 0) {
            showEmpty(getString(R.string.history_empty_login_required))
            binding.btnClearAll.visibility = View.GONE
            return
        }

        showLoading()
        when (activeTab) {
            ActiveTab.FAVORIT -> loadFavorit(userId)
            ActiveTab.PENCARIAN -> loadSearchHistory(userId)
            ActiveTab.PERJALANAN -> loadRouteHistory(userId)
        }
    }

    private fun loadFavorit(userId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val trips = withContext(Dispatchers.IO) { savedTripDao.getByUser(userId) }
            hideLoading()
            if (trips.isEmpty()) {
                showEmpty(getString(R.string.history_favorit_empty))
                binding.btnClearAll.visibility = View.GONE
            } else {
                binding.rvHistory.adapter = savedTripAdapter
                savedTripAdapter.submitList(trips)
                showList()
                binding.btnClearAll.visibility = View.GONE // favorit tidak ada hapus semua
            }
        }
    }

    private fun loadSearchHistory(userId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val history = withContext(Dispatchers.IO) { historyDao.getSearchHistory(userId) }
            hideLoading()
            if (history.isEmpty()) {
                showEmpty(getString(R.string.history_pencarian_empty))
                binding.btnClearAll.visibility = View.GONE
            } else {
                binding.rvHistory.adapter = searchHistoryAdapter
                searchHistoryAdapter.submitList(history)
                showList()
                binding.btnClearAll.visibility = View.VISIBLE
            }
        }
    }

    private fun loadRouteHistory(userId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val history = withContext(Dispatchers.IO) { historyDao.getRouteHistory(userId) }
            hideLoading()
            if (history.isEmpty()) {
                showEmpty(getString(R.string.history_perjalanan_empty))
                binding.btnClearAll.visibility = View.GONE
            } else {
                binding.rvHistory.adapter = routeHistoryAdapter
                routeHistoryAdapter.submitList(history)
                showList()
                binding.btnClearAll.visibility = View.VISIBLE
            }
        }
    }

    // ── Aksi Favorit ─────────────────────────────────────────────────────────

    private fun replayFavorit(trip: SavedTrip) {
        HistoryReplayRequest.pendingOrigin = LocationPoint(
            label = trip.originName,
            latitude = trip.originLat,
            longitude = trip.originLon,
            isFromGps = false
        )
        HistoryReplayRequest.pendingDestination = SearchLocation(
            name = trip.destinationName,
            address = null,
            latitude = trip.destinationLat,
            longitude = trip.destinationLon
        )
        HistoryReplayRequest.pendingMode = trip.mode
        HistoryReplayRequest.pendingPriority = trip.priority

        // Navigasi ke Home
        findNavController().navigate(R.id.homeFragment)
        toast(getString(R.string.history_replay_started))
    }

    private fun showEditFavoritDialog(trip: SavedTrip) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(android.R.layout.simple_list_item_2, null)

        val etName = EditText(requireContext()).apply {
            hint = getString(R.string.history_favorit_name_hint)
            setText(trip.namaPerjalanan)
        }
        val etCatatan = EditText(requireContext()).apply {
            hint = getString(R.string.history_favorit_catatan_hint)
            setText(trip.catatan ?: "")
        }

        val container = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0)
            addView(etName)
            addView(etCatatan)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.history_favorit_edit_title))
            .setView(container)
            .setPositiveButton(getString(R.string.history_save)) { _, _ ->
                val newName = etName.text.toString().trim()
                val newCatatan = etCatatan.text.toString().trim().ifEmpty { null }
                if (newName.isBlank()) {
                    toast(getString(R.string.history_favorit_name_required))
                    return@setPositiveButton
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        savedTripDao.updateNameAndNote(trip.idSaved, newName, newCatatan)
                    }
                    toast(getString(R.string.history_favorit_updated))
                    loadCurrentTab()
                }
            }
            .setNegativeButton(getString(R.string.history_cancel), null)
            .show()
    }

    private fun confirmDeleteFavorit(trip: SavedTrip) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.history_delete_confirm_title))
            .setMessage(getString(R.string.history_favorit_delete_message, trip.namaPerjalanan))
            .setPositiveButton(getString(R.string.history_delete)) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) { savedTripDao.delete(trip.idSaved) }
                    toast(getString(R.string.history_favorit_deleted))
                    loadCurrentTab()
                }
            }
            .setNegativeButton(getString(R.string.history_cancel), null)
            .show()
    }

    // ── Aksi Riwayat Pencarian ────────────────────────────────────────────────

    private fun useSearchHistoryAsDestination(history: SearchHistory) {
        HistoryReplayRequest.pendingDestination = SearchLocation(
            name = history.selectedName,
            address = history.selectedAddress,
            latitude = history.selectedLat,
            longitude = history.selectedLon
        )
        HistoryReplayRequest.pendingOrigin = null
        HistoryReplayRequest.pendingMode = null
        HistoryReplayRequest.pendingPriority = null

        findNavController().navigate(R.id.homeFragment)
        toast(getString(R.string.history_search_applied))
    }

    private fun deleteSearchHistory(history: SearchHistory) {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) { historyDao.deleteSearchHistory(history.idSearch) }
            loadCurrentTab()
        }
    }

    // ── Aksi Riwayat Perjalanan ───────────────────────────────────────────────

    private fun deleteRouteHistory(history: com.example.naikapa.data.model.RouteHistory) {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) { historyDao.deleteRouteHistory(history.idHistory) }
            loadCurrentTab()
        }
    }

    // ── Hapus Semua ───────────────────────────────────────────────────────────

    private fun confirmClearAll() {
        val userId = sessionManager.getUserId()
        if (userId <= 0) return

        val message = when (activeTab) {
            ActiveTab.PENCARIAN -> getString(R.string.history_clear_pencarian_confirm)
            ActiveTab.PERJALANAN -> getString(R.string.history_clear_perjalanan_confirm)
            else -> return
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.history_clear_all_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.history_clear_all)) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        when (activeTab) {
                            ActiveTab.PENCARIAN -> historyDao.clearSearchHistory(userId)
                            ActiveTab.PERJALANAN -> historyDao.clearRouteHistory(userId)
                            else -> Unit
                        }
                    }
                    toast(getString(R.string.history_cleared))
                    loadCurrentTab()
                }
            }
            .setNegativeButton(getString(R.string.history_cancel), null)
            .show()
    }

    // ── UI State Helpers ──────────────────────────────────────────────────────

    private fun showLoading() {
        binding.progressHistory.visibility = View.VISIBLE
        binding.cardEmptyState.visibility = View.GONE
        binding.rvHistory.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.progressHistory.visibility = View.GONE
    }

    private fun showEmpty(message: String) {
        binding.tvEmptySubtitle.text = message
        binding.cardEmptyState.visibility = View.VISIBLE
        binding.rvHistory.visibility = View.GONE
    }

    private fun showList() {
        binding.cardEmptyState.visibility = View.GONE
        binding.rvHistory.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::dbHelper.isInitialized) dbHelper.close()
        _binding = null
    }
}
