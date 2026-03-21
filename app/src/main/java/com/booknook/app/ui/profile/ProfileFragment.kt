package com.booknook.app.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
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
            viewModel.inputAvatarUri.value = it
            Picasso.get().load(it).fit().centerCrop().into(binding.avatarImage)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        observeViewModel()

        binding.usernameInput.doOnTextChanged { text, _, _, _ ->
            viewModel.inputUsername.value = text?.toString().orEmpty()
        }

        binding.pickAvatarBtn.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.saveBtn.setOnClickListener {
            val username = binding.usernameInput.text.toString().trim()
            val user = viewModel.user.value
            if (username.isNotEmpty() && user != null) {
                viewModel.updateProfile(username, user.email, selectedAvatarUri)
            } else if (username.isEmpty()) {
                Snackbar.make(binding.root, getString(R.string.profile_required_error), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
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
                if (binding.usernameInput.text.isNullOrEmpty()) {
                    binding.usernameInput.setText(it.username)
                }
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

        viewModel.isChanged.observe(viewLifecycleOwner) { isChanged ->
            binding.saveBtn.isEnabled = isChanged && viewModel.loading.value != true
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.loading.isVisible = isLoading
            binding.saveBtn.isEnabled = !isLoading && viewModel.isChanged.value == true
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, getString(it), Snackbar.LENGTH_SHORT).show()
            }
        }

        viewModel.updateSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Snackbar.make(binding.root, R.string.profile_updated, Snackbar.LENGTH_SHORT).show()
                viewModel.resetUpdateSuccess()
                selectedAvatarUri = null
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
