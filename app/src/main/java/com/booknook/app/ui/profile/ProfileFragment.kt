package com.booknook.app.ui.profile

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.booknook.app.R
import com.booknook.app.base.MyApplication
import com.booknook.app.databinding.FragmentProfileBinding
import com.booknook.app.util.IMAGE_PICKER_MIME_TYPE
import com.booknook.app.util.loadLocalImage
import com.booknook.app.util.loadRemoteImage
import com.google.android.material.snackbar.Snackbar

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as MyApplication
    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModel.factory(
            profileRepository = app.profileRepository,
            authRepository = app.authRepository
        )
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            viewModel.onAvatarSelected(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        observeViewModel()

        binding.usernameInput.doOnTextChanged { text, _, _, _ ->
            viewModel.onUsernameChanged(text?.toString().orEmpty())
        }

        binding.pickAvatarBtn.setOnClickListener {
            pickImageLauncher.launch(IMAGE_PICKER_MIME_TYPE)
        }

        binding.saveBtn.setOnClickListener {
            viewModel.onSaveRequested()
        }

        binding.logoutBtn.setOnClickListener {
            viewModel.onLogoutRequested()
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (binding.usernameInput.text?.toString() != state.draftUsername) {
                binding.usernameInput.setText(state.draftUsername)
                binding.usernameInput.setSelection(state.draftUsername.length)
            }

            binding.emailInput.setText(state.user?.email.orEmpty())

            when {
                state.selectedAvatarUri != null -> {
                    binding.avatarImage.loadLocalImage(state.selectedAvatarUri, R.drawable.ic_default_avatar)
                }

                !state.user?.avatarUrl.isNullOrBlank() -> {
                    binding.avatarImage.loadRemoteImage(
                        state.user?.avatarUrl,
                        R.drawable.ic_default_avatar
                    )
                }

                else -> binding.avatarImage.setImageResource(R.drawable.ic_default_avatar)
            }

            val isBusy = state.isSaving || state.isLoggingOut
            binding.loading.isVisible = isBusy
            binding.saveBtn.isEnabled = state.canSave && !isBusy
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { action ->
                when (action) {
                    ProfileEvent.NavigateToLogin -> {
                        findNavController().navigate(R.id.action_global_logout)
                    }

                    is ProfileEvent.ShowMessage -> {
                        Snackbar.make(binding.root, getString(action.messageRes), Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
