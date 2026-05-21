package com.example.naikapa.presentation.profile

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.naikapa.R
import com.example.naikapa.common.SessionManager
import com.example.naikapa.common.toast
import com.example.naikapa.data.local.NaikApaDatabaseHelper
import com.example.naikapa.data.local.UserDao
import com.example.naikapa.data.model.User
import com.example.naikapa.databinding.FragmentProfilBinding
import com.example.naikapa.presentation.auth.AuthActivity

class ProfilFragment : Fragment() {

    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private lateinit var dbHelper: NaikApaDatabaseHelper
    private lateinit var userDao: UserDao
    private var activeUser: User? = null

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
        dbHelper = NaikApaDatabaseHelper(requireContext())
        userDao = UserDao(dbHelper)

        loadActiveUser()

        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        binding.btnDeleteAccount.setOnClickListener {
            confirmDeleteAccount()
        }

        binding.btnLogout.setOnClickListener {
            sessionManager.logout()
            toast("Logout berhasil")
            goToAuth()
        }
    }

    private fun loadActiveUser() {
        val userId = sessionManager.getUserId()
        val user = if (userId > 0) userDao.getUserById(userId) else null
        if (user == null) {
            sessionManager.logout()
            toast("Sesi tidak valid, silakan login ulang")
            goToAuth()
            return
        }

        activeUser = user
        bindUser(user)
    }

    private fun bindUser(user: User) {
        binding.tvProfileName.text = user.nama
        binding.tvProfileEmail.text = user.email
        binding.etProfileName.setText(user.nama)
        binding.etProfileEmail.setText(user.email)
        binding.etProfilePassword.setText("")
        binding.switchMotor.isChecked = user.hasMotor
        binding.switchCar.isChecked = user.hasCar
        binding.tvMotorStatus.text = "Motor: ${if (user.hasMotor) "Ya" else "Tidak"}"
        binding.tvCarStatus.text = "Mobil: ${if (user.hasCar) "Ya" else "Tidak"}"
    }

    private fun saveProfile() {
        val currentUser = activeUser ?: return
        val name = binding.etProfileName.text.toString().trim()
        val email = binding.etProfileEmail.text.toString().trim()
        val newPassword = binding.etProfilePassword.text.toString().trim()

        if (name.isEmpty()) {
            toast("Nama tidak boleh kosong")
            return
        }
        if (email.isEmpty()) {
            toast("Email tidak boleh kosong")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Format email tidak valid")
            return
        }
        if (newPassword.isNotEmpty() && newPassword.length < 6) {
            toast("Password minimal 6 karakter")
            return
        }
        if (userDao.isEmailUsedByOtherUser(email, currentUser.idUser)) {
            toast("Email sudah digunakan akun lain")
            return
        }

        val updatedUser = currentUser.copy(
            nama = name,
            email = email,
            password = newPassword.ifEmpty { currentUser.password },
            hasMotor = binding.switchMotor.isChecked,
            hasCar = binding.switchCar.isChecked
        )

        val updatedRows = userDao.updateUser(updatedUser)
        if (updatedRows <= 0) {
            toast("Gagal menyimpan profil")
            return
        }

        activeUser = updatedUser
        sessionManager.updateSessionUser(updatedUser.nama, updatedUser.email)
        bindUser(updatedUser)
        toast("Profil berhasil diperbarui")
    }

    private fun confirmDeleteAccount() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile_delete_title)
            .setMessage(R.string.profile_delete_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteAccount()
            }
            .show()
    }

    private fun deleteAccount() {
        val userId = activeUser?.idUser ?: sessionManager.getUserId()
        val deletedRows = userDao.deleteUser(userId)
        if (deletedRows <= 0) {
            toast("Gagal menghapus akun")
            return
        }
        sessionManager.logout()
        toast("Akun lokal berhasil dihapus")
        goToAuth()
    }

    private fun goToAuth() {
        startActivity(Intent(requireActivity(), AuthActivity::class.java))
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::dbHelper.isInitialized) dbHelper.close()
        _binding = null
    }
}
