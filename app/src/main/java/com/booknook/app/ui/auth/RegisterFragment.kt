package com.booknook.app.ui.auth

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.booknook.app.R
import com.booknook.app.base.MyApplication
import com.booknook.app.databinding.FragmentRegisterBinding
import com.booknook.app.util.IMAGE_PICKER_MIME_TYPE
import com.booknook.app.util.loadLocalImage
import com.google.android.material.snackbar.Snackbar

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as MyApplication
    private val viewModel: AuthViewModel by viewModels {
        AuthViewModel.factory(app.authRepository)
    }
    
    private var selectedAvatarUri: Uri? = null
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedAvatarUri = it
            binding.ivAvatar.loadLocalImage(it, R.drawable.ic_default_avatar)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRegisterBinding.bind(view)
        binding.ivAvatar.setImageResource(R.drawable.ic_default_avatar)

        observeViewModel()

        binding.btnPickAvatar.setOnClickListener {
            pickImageLauncher.launch(IMAGE_PICKER_MIME_TYPE)
        }

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val username = binding.etUsername.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
                Snackbar.make(binding.root, R.string.register_required_error, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.onRegisterSubmitted(email, password, username, selectedAvatarUri)
        }

        binding.tvLogin.setOnClickListener {
            viewModel.onBackToLoginClicked()
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            setLoading(state.isLoading)
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { error ->
                when (error) {
                    AuthEvent.NavigateToRegister -> Unit

                    AuthEvent.NavigateBackToLogin -> {
                        findNavController().popBackStack()
                    }

                    is AuthEvent.NavigateToPosts -> {
                        Snackbar.make(binding.root, error.messageRes, Snackbar.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_registerFragment_to_postsFragment)
                    }

                    is AuthEvent.ShowMessage -> {
                        Snackbar.make(binding.root, getString(error.messageRes), Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnRegister.isEnabled = !isLoading
        binding.btnPickAvatar.isEnabled = !isLoading
        binding.pbLoading.isVisible = isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
