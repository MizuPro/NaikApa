package com.example.naikapa.presentation.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.naikapa.common.SessionManager
import com.example.naikapa.common.toast
import com.example.naikapa.databinding.FragmentProfilBinding
import com.example.naikapa.presentation.auth.AuthActivity

class ProfilFragment : Fragment() {

    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        binding.tvProfileName.text = sessionManager.getUserName() ?: "User NaikApa"
        binding.tvProfileEmail.text = sessionManager.getUserEmail() ?: "email@example.com"

        binding.btnLogout.setOnClickListener {
            sessionManager.logout()
            toast("Logout berhasil")
            startActivity(Intent(requireActivity(), AuthActivity::class.java))
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
