package com.example.naikapa.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.naikapa.MainActivity
import com.example.naikapa.common.SessionManager
import com.example.naikapa.common.toast
import com.example.naikapa.data.local.NaikApaDatabaseHelper
import com.example.naikapa.data.local.UserDao
import com.example.naikapa.data.model.User
import com.example.naikapa.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private lateinit var dbHelper: NaikApaDatabaseHelper
    private lateinit var userDao: UserDao

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
        dbHelper = NaikApaDatabaseHelper(requireContext())
        userDao = UserDao(dbHelper)

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
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

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
            if (password.length < 6) {
                toast("Password minimal 6 karakter")
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
            if (userDao.isEmailExists(email)) {
                toast("Email sudah terdaftar")
                return@setOnClickListener
            }

            val userId = userDao.insertUser(
                User(
                    nama = name,
                    email = email,
                    password = password,
                    hasMotor = binding.cbMotor.isChecked,
                    hasCar = binding.cbMobil.isChecked
                )
            )
            if (userId <= 0) {
                toast("Registrasi gagal")
                return@setOnClickListener
            }

            sessionManager.saveSession(userId, name, email)
            toast("Registrasi berhasil")
            startActivity(Intent(requireActivity(), MainActivity::class.java))
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::dbHelper.isInitialized) dbHelper.close()
        _binding = null
    }
}
