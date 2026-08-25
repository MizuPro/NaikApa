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
import com.example.naikapa.data.local.NaikApaDatabaseHelper
import com.example.naikapa.data.local.UserDao
import com.example.naikapa.data.repository.AuthRepository
import com.example.naikapa.databinding.FragmentLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var dbHelper: NaikApaDatabaseHelper
    private lateinit var authRepository: AuthRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        dbHelper = NaikApaDatabaseHelper(requireContext())
        authRepository = AuthRepository(
            sessionManager = sessionManager,
            userDao = UserDao(dbHelper)
        )

        binding.tvRegisterLink.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.btnLogin.setOnClickListener {
            val email    = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

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

            setLoading(true)

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val result = authRepository.login(email, password)

                withContext(Dispatchers.Main) {
                    setLoading(false)
                    result.fold(
                        onSuccess = {
                            toast("Login berhasil")
                            startActivity(Intent(requireActivity(), MainActivity::class.java))
                            requireActivity().finish()
                        },
                        onFailure = { err ->
                            toast(err.message ?: "Login gagal. Coba lagi.")
                        }
                    )
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnLogin.isEnabled = !isLoading
        binding.btnLogin.text = if (isLoading) "Memproses..." else getString(R.string.login_button)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::dbHelper.isInitialized) dbHelper.close()
        _binding = null
    }
}
