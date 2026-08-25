package com.example.naikapa.presentation.report

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.naikapa.R
import com.example.naikapa.common.SessionManager
import com.example.naikapa.common.toast
import com.example.naikapa.data.local.DisruptionReportDao
import com.example.naikapa.data.local.NaikApaDatabaseHelper
import com.example.naikapa.data.model.DisruptionReport
import com.example.naikapa.databinding.FragmentStatusGangguanBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatusGangguanFragment : Fragment() {

    private var _binding: FragmentStatusGangguanBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var dbHelper: NaikApaDatabaseHelper
    private lateinit var reportDao: DisruptionReportDao
    private lateinit var delayRepository: com.example.naikapa.data.repository.DelayRepository
    private lateinit var adapter: DisruptionReportAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatusGangguanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        dbHelper = NaikApaDatabaseHelper(requireContext())
        reportDao = DisruptionReportDao(dbHelper)
        delayRepository = com.example.naikapa.data.repository.DelayRepository(disruptionReportDao = reportDao)

        setupRecyclerView()
        setupFab()
    }

    override fun onResume() {
        super.onResume()
        loadReports()
    }

    private fun setupRecyclerView() {
        val userId = sessionManager.getUserId()
        adapter = DisruptionReportAdapter(
            currentUserId = userId,
            onEditClick   = { report -> navigateToEditForm(report) },
            onDeleteClick = { report -> confirmDelete(report) }
        )
        binding.rvReports.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@StatusGangguanFragment.adapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupFab() {
        binding.fabAddReport.setOnClickListener {
            val userId = sessionManager.getUserId()
            if (userId <= 0) {
                toast(getString(R.string.report_login_required))
                return@setOnClickListener
            }
            findNavController().navigate(
                R.id.addEditDisruptionReportFragment,
                Bundle().apply {
                    putLong(AddEditDisruptionReportFragment.ARG_REPORT_ID, 0L)
                }
            )
        }
    }

    private fun loadReports() {
        binding.progressReports.visibility = View.VISIBLE
        binding.rvReports.visibility       = View.GONE
        binding.cardEmptyState.visibility  = View.GONE

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val reports: List<DisruptionReport> = try {
                delayRepository.getActiveReports()
            } catch (e: Exception) {
                emptyList()
            }
            withContext(Dispatchers.Main) {
                binding.progressReports.visibility = View.GONE
                if (reports.isEmpty()) {
                    binding.cardEmptyState.visibility = View.VISIBLE
                    binding.rvReports.visibility      = View.GONE
                } else {
                    binding.cardEmptyState.visibility = View.GONE
                    binding.rvReports.visibility      = View.VISIBLE
                    adapter.submitList(reports)
                }
            }
        }
    }

    private fun navigateToEditForm(report: DisruptionReport) {
        findNavController().navigate(
            R.id.addEditDisruptionReportFragment,
            Bundle().apply {
                putLong(AddEditDisruptionReportFragment.ARG_REPORT_ID, report.idReport)
            }
        )
    }

    private fun confirmDelete(report: DisruptionReport) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.report_delete_title)
            .setMessage(R.string.report_delete_message)
            .setPositiveButton(R.string.delete) { _, _ -> deleteReport(report) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteReport(report: DisruptionReport) {
        val userId = sessionManager.getUserId()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val success = delayRepository.deleteReport(report.idReport, userId)
            if (success) {
                ReportPhotoHelper.deletePhoto(report.photoPath)
            }
            withContext(Dispatchers.Main) {
                if (success) {
                    toast(getString(R.string.report_deleted_success))
                    loadReports()
                } else {
                    toast(getString(R.string.report_delete_failed))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::dbHelper.isInitialized) dbHelper.close()
        _binding = null
    }
}
