package com.booknook.app.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.booknook.app.R
import com.booknook.app.databinding.FragmentProfileBinding
import com.booknook.app.model.Model
import com.booknook.app.ui.auth.LoginActivity
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var binding: FragmentProfileBinding
    private var pickedAvatar: Uri? = null

    private val pickAvatar = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pickedAvatar = uri
        if (uri != null) Toast.makeText(requireContext(), "Avatar selected", Toast.LENGTH_SHORT).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentProfileBinding.bind(view)

        Model.observeLocalUser().observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.usernameInput.setText(user.username)
                binding.emailInput.setText(user.email)
            }
        }

        binding.pickAvatarBtn.setOnClickListener { pickAvatar.launch("image/*") }

        binding.saveBtn.setOnClickListener {
            val username = binding.usernameInput.text.toString().trim()
            val email = binding.emailInput.text.toString().trim()
            if (username.isEmpty() || email.isEmpty()) {
                Toast.makeText(requireContext(), "Username and email required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            setLoading(true)
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    Model.updateProfile(username, email, pickedAvatar)
                    Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), e.message ?: "Save failed", Toast.LENGTH_SHORT).show()
                } finally {
                    setLoading(false)
                }
            }
        }

        binding.logoutBtn.setOnClickListener {
            Model.logout()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.saveBtn.isEnabled = !isLoading
    }
}
