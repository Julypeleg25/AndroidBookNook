package com.booknook.app.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.booknook.app.R
import com.booknook.app.databinding.FragmentProfileBinding
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private var selectedAvatarUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedAvatarUri = it
            Picasso.get().load(it).fit().centerCrop().into(binding.avatarImage)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        observeViewModel()

        binding.pickAvatarBtn.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.saveBtn.setOnClickListener {
            val username = binding.usernameInput.text.toString().trim()
            val email = binding.emailInput.text.toString().trim()
            if (username.isNotEmpty() && email.isNotEmpty()) {
                viewModel.updateProfile(username, email, selectedAvatarUri)
            } else {
                Snackbar.make(binding.root, "All fields are required", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.logoutBtn.setOnClickListener {
            viewModel.logout()
            findNavController().navigate(R.id.action_global_logout)
        }
    }

    private fun observeViewModel() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.usernameInput.setText(it.username)
                binding.emailInput.setText(it.email)
                if (selectedAvatarUri == null && !it.avatarUrl.isNullOrBlank()) {
                    Picasso.get()
                        .load(it.avatarUrl)
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .fit()
                        .centerCrop()
                        .into(binding.avatarImage)
                }
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.loading.isVisible = isLoading
            binding.saveBtn.isEnabled = !isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
            }
        }

        viewModel.updateSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Snackbar.make(binding.root, "Profile updated", Snackbar.LENGTH_SHORT).show()
                viewModel.resetUpdateSuccess()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
