package com.booknook.app.ui.auth

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.booknook.app.R
import com.booknook.app.base.MyApplication
import com.booknook.app.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as MyApplication
    private val viewModel: AuthViewModel by viewModels {
        AuthViewModel.factory(app.authRepository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        observeViewModel()

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Snackbar.make(binding.root, R.string.login_required_error, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.onLoginSubmitted(email, password)
        }

        binding.tvRegister.setOnClickListener {
            viewModel.onRegisterLinkClicked()
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            setLoading(state.isLoading)
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { error ->
                when (error) {
                    AuthEvent.NavigateToRegister -> {
                        findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
                    }

                    AuthEvent.NavigateBackToLogin -> Unit

                    is AuthEvent.NavigateToPosts -> {
                        Snackbar.make(binding.root, error.messageRes, Snackbar.LENGTH_SHORT).show()
                        navigateToPosts()
                    }

                    is AuthEvent.ShowMessage -> {
                        Snackbar.make(binding.root, getString(error.messageRes), Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnLogin.isEnabled = !isLoading
        binding.pbLoading.isVisible = isLoading
    }

    private fun navigateToPosts() {
        findNavController().navigate(R.id.action_loginFragment_to_postsFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
