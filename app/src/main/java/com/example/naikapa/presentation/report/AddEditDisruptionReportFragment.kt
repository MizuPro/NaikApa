package com.example.naikapa.presentation.report

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.naikapa.R
import com.example.naikapa.common.AppConstants
import com.example.naikapa.common.SessionManager
import com.example.naikapa.common.toast
import com.example.naikapa.data.local.DisruptionReportDao
import com.example.naikapa.data.local.NaikApaDatabaseHelper
import com.example.naikapa.data.model.DisruptionReport
import com.example.naikapa.databinding.FragmentAddEditDisruptionReportBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AddEditDisruptionReportFragment : Fragment() {

    private var _binding: FragmentAddEditDisruptionReportBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var dbHelper: NaikApaDatabaseHelper
    private lateinit var reportDao: DisruptionReportDao

    private var existingReport: DisruptionReport? = null
    private var photoFile: File? = null
    private var photoUri: Uri? = null
    private var selectedPhotoPath: String? = null

    // ── Activity result launchers ─────────────────────────────────────────────

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoFile != null) {
            selectedPhotoPath = ReportPhotoHelper.normalizePath(photoFile)
            showPhotoPreview(Uri.fromFile(photoFile))
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val destFile = ReportPhotoHelper.createPhotoFile(requireContext())
                try {
                    requireContext().contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    photoFile = destFile
                    selectedPhotoPath = ReportPhotoHelper.normalizePath(destFile)
                    withContext(Dispatchers.Main) {
                        showPhotoPreview(Uri.fromFile(destFile))
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        toast(getString(R.string.report_photo_copy_failed))
                    }
                }
            }
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera() else toast(getString(R.string.report_permission_camera_denied))
    }

    private val galleryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchGallery() else toast(getString(R.string.report_permission_gallery_denied))
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditDisruptionReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        dbHelper = NaikApaDatabaseHelper(requireContext())
        reportDao = DisruptionReportDao(dbHelper)

        setupCategoryDropdown()
        setupButtons()
        prefillFromArgs()
        loadExistingReportIfEdit()
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            AppConstants.DISRUPTION_CATEGORIES
        )
        binding.actvCategory.setAdapter(adapter)
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.btnCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                launchCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnGallery.setOnClickListener {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            if (ContextCompat.checkSelfPermission(requireContext(), permission)
                == PackageManager.PERMISSION_GRANTED
            ) {
                launchGallery()
            } else {
                galleryPermissionLauncher.launch(permission)
            }
        }

        binding.btnRemovePhoto.setOnClickListener {
            selectedPhotoPath = null
            photoFile = null
            binding.cardPhotoPreview.visibility = View.GONE
        }

        binding.btnSave.setOnClickListener { validateAndSave() }
    }

    private fun prefillFromArgs() {
        val prefillStop  = arguments?.getString(ARG_STOP_ID)
        val prefillRoute = arguments?.getString(ARG_ROUTE_ID)
        if (!prefillStop.isNullOrBlank())  binding.etStop.setText(prefillStop)
        if (!prefillRoute.isNullOrBlank()) binding.etRoute.setText(prefillRoute)
    }

    private fun loadExistingReportIfEdit() {
        val reportId = arguments?.getLong(ARG_REPORT_ID, 0L) ?: 0L
        if (reportId <= 0L) {
            binding.tvToolbarTitle.text = getString(R.string.report_form_title_add)
            return
        }
        binding.tvToolbarTitle.text = getString(R.string.report_form_title_edit)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val report = reportDao.getById(reportId)
            withContext(Dispatchers.Main) {
                if (report != null) {
                    existingReport = report
                    binding.actvCategory.setText(report.category, false)
                    binding.etStop.setText(report.stopId ?: "")
                    binding.etRoute.setText(report.routeId ?: "")
                    binding.etDescription.setText(report.description)
                    if (!report.photoPath.isNullOrBlank()) {
                        val file = File(report.photoPath)
                        if (file.exists()) {
                            selectedPhotoPath = report.photoPath
                            photoFile = file
                            showPhotoPreview(Uri.fromFile(file))
                        }
                    }
                }
            }
        }
    }

    // ── Camera / Gallery ──────────────────────────────────────────────────────

    private fun launchCamera() {
        val file = ReportPhotoHelper.createPhotoFile(requireContext())
        photoFile = file
        photoUri  = ReportPhotoHelper.getUriForFile(requireContext(), file)
        cameraLauncher.launch(photoUri)
    }

    private fun launchGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun showPhotoPreview(uri: Uri) {
        binding.imgPhotoPreview.setImageURI(uri)
        binding.cardPhotoPreview.visibility = View.VISIBLE
    }

    // ── Validation & Save ─────────────────────────────────────────────────────

    private fun validateAndSave() {
        val category    = binding.actvCategory.text.toString().trim()
        val stopId      = binding.etStop.text.toString().trim().ifBlank { null }
        val routeId     = binding.etRoute.text.toString().trim().ifBlank { null }
        val description = binding.etDescription.text.toString().trim()

        var valid = true

        if (category.isBlank()) {
            binding.tilCategory.error = getString(R.string.report_error_category_required)
            valid = false
        } else {
            binding.tilCategory.error = null
        }

        if (description.length < AppConstants.DISRUPTION_DESCRIPTION_MIN_LENGTH) {
            binding.tilDescription.error = getString(
                R.string.report_error_description_too_short,
                AppConstants.DISRUPTION_DESCRIPTION_MIN_LENGTH
            )
            valid = false
        } else if (description.length > AppConstants.DISRUPTION_DESCRIPTION_MAX_LENGTH) {
            binding.tilDescription.error = getString(R.string.report_error_description_too_long)
            valid = false
        } else {
            binding.tilDescription.error = null
        }

        if (!valid) return

        val userId = sessionManager.getUserId()
        if (userId <= 0) {
            toast(getString(R.string.report_login_required))
            return
        }

        binding.btnSave.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val existing = existingReport
            val result: Boolean = if (existing != null) {
                val updated = existing.copy(
                    category    = category,
                    stopId      = stopId,
                    routeId     = routeId,
                    description = description,
                    photoPath   = selectedPhotoPath ?: existing.photoPath
                )
                reportDao.updateByUser(updated) > 0
            } else {
                val now = System.currentTimeMillis()
                val report = DisruptionReport(
                    idUser      = userId,
                    stopId      = stopId,
                    routeId     = routeId,
                    category    = category,
                    description = description,
                    photoPath   = selectedPhotoPath,
                    createdAt   = now,
                    expiredAt   = now + DisruptionReport.ONE_HOUR_MILLIS
                )
                reportDao.insert(report) > 0
            }

            withContext(Dispatchers.Main) {
                binding.btnSave.isEnabled = true
                if (result) {
                    toast(getString(
                        if (existing != null) R.string.report_updated_success
                        else R.string.report_saved_success
                    ))
                    findNavController().popBackStack()
                } else {
                    toast(getString(R.string.report_save_failed))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::dbHelper.isInitialized) dbHelper.close()
        _binding = null
    }

    companion object {
        const val ARG_REPORT_ID = "reportId"
        const val ARG_STOP_ID   = "stopId"
        const val ARG_ROUTE_ID  = "routeId"
    }
}
