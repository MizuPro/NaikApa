package com.example.naikapa.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.naikapa.MainActivity
import com.example.naikapa.common.SessionManager
import com.example.naikapa.common.toast
import com.example.naikapa.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

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

        binding.tvLoginLink.setOnClickListener {
            findNavController().popBackStack()
        }

        // Toggling checkboxes when the preference cards are clicked (premium UX)
        binding.cardMotor.setOnClickListener {
            binding.cbMotor.isChecked = !binding.cbMotor.isChecked
        }
        binding.cardMobil.setOnClickListener {
            binding.cbMobil.isChecked = !binding.cbMobil.isChecked
        }

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (name.isEmpty()) {
                toast("Nama tidak boleh kosong")
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                toast("Email tidak boleh kosong")
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                toast("Password tidak boleh kosong")
                return@setOnClickListener
            }

            // Placeholder register success (will save to SQLite in Phase 4)
            sessionManager.saveSession(1, name, email)
            toast("Registrasi berhasil (Demo)")
            startActivity(Intent(requireActivity(), MainActivity::class.java))
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
