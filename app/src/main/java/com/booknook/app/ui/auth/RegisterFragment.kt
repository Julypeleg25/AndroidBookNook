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
import com.booknook.app.databinding.FragmentRegisterBinding
import com.google.android.material.snackbar.Snackbar

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    
    private var selectedAvatarUri: Uri? = null
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedAvatarUri = it
            binding.ivAvatar.setImageURI(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRegisterBinding.bind(view)

        observeViewModel()

        binding.btnPickAvatar.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val username = binding.etUsername.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
                Snackbar.make(binding.root, R.string.register_required_error, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.register(email, password, username, selectedAvatarUri)
        }

        binding.tvLogin.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun observeViewModel() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            setLoading(isLoading)
        }

        viewModel.error.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { error ->
                Snackbar.make(binding.root, getString(error), Snackbar.LENGTH_SHORT).show()
            }
        }

        viewModel.registrationSuccess.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                Snackbar.make(binding.root, R.string.register_success_message, Snackbar.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_registerFragment_to_postsFragment)
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
