package com.colman.booknook.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.colman.booknook.R
import com.colman.booknook.databinding.FragmentRegisterBinding
import com.colman.booknook.model.Model
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val username = binding.etUsername.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty() && username.isNotEmpty()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        Model.firebase.register(email, password, username)
                        Model.ensureLocalProfile()
                        Toast.makeText(requireContext(), "Registration Successful", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.postsFragment)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Registration Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.tvLogin.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
