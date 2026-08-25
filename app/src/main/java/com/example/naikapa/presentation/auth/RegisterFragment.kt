package com.example.naikapa.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.naikapa.MainActivity
import com.example.naikapa.R
import com.example.naikapa.common.SessionManager
import com.example.naikapa.common.toast
import com.example.naikapa.data.repository.AuthRepository
import com.example.naikapa.databinding.FragmentRegisterBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var authRepository: AuthRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        authRepository = AuthRepository(sessionManager = sessionManager)

        binding.tvLoginLink.setOnClickListener {
            findNavController().popBackStack()
        }

        // Toggling checkboxes ketika card diklik (premium UX)
        binding.cardMotor.setOnClickListener {
            binding.cbMotor.isChecked = !binding.cbMotor.isChecked
            updateCardState(binding.cardMotor, binding.cbMotor.isChecked)
        }
        binding.cardMobil.setOnClickListener {
            binding.cbMobil.isChecked = !binding.cbMobil.isChecked
            updateCardState(binding.cardMobil, binding.cbMobil.isChecked)
        }

        binding.btnRegister.setOnClickListener {
            val name            = binding.etName.text.toString().trim()
            val email           = binding.etEmail.text.toString().trim()
            val password        = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            val hasMotor        = binding.cbMotor.isChecked
            val hasCar          = binding.cbMobil.isChecked

            // ── Validasi input sisi client ──────────────────────────────────
            if (name.isEmpty()) {
                toast("Nama tidak boleh kosong")
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                toast("Email tidak boleh kosong")
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                toast("Format email tidak valid")
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                toast("Password tidak boleh kosong")
                return@setOnClickListener
            }
            if (password.length < 8) {
                toast("Password minimal 8 karakter")
                return@setOnClickListener
            }
            if (confirmPassword.isEmpty()) {
                toast("Konfirmasi password tidak boleh kosong")
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                toast("Konfirmasi password tidak sama")
                return@setOnClickListener
            }

            setLoading(true)

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val result = authRepository.register(
                    nama = name,
                    email = email,
                    password = password,
                    hasMotor = hasMotor,
                    hasCar = hasCar
                )

                withContext(Dispatchers.Main) {
                    setLoading(false)
                    result.fold(
                        onSuccess = {
                            toast("Registrasi berhasil")
                            startActivity(Intent(requireActivity(), MainActivity::class.java))
                            requireActivity().finish()
                        },
                        onFailure = { err ->
                            toast(err.message ?: "Registrasi gagal. Coba lagi.")
                        }
                    )
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnRegister.isEnabled = !isLoading
        binding.btnRegister.text = if (isLoading) "Memproses..." else getString(R.string.register_button)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateCardState(card: com.google.android.material.card.MaterialCardView, isChecked: Boolean) {
        val context = requireContext()
        if (isChecked) {
            card.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.colorPrimaryLight))
            card.strokeColor = androidx.core.content.ContextCompat.getColor(context, R.color.colorPrimary)
            card.strokeWidth = resources.getDimensionPixelSize(R.dimen.stroke_medium)
        } else {
            card.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.colorSurface))
            card.strokeColor = androidx.core.content.ContextCompat.getColor(context, R.color.colorCardOutline)
            card.strokeWidth = resources.getDimensionPixelSize(R.dimen.stroke_thin)
        }
    }
}
