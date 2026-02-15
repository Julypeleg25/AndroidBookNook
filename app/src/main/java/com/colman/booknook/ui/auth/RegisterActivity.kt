package com.colman.booknook.app.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.colman.booknook.app.databinding.ActivityRegisterBinding
import com.colman.booknook.app.model.Model
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginBtn.text = "Register"
        binding.registerLink.text = "Have an account? Login"

        binding.loginBtn.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()
            val username = "User" // simplest; you can add a username field in layout later

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email and password required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            setLoading(true)
            lifecycleScope.launch {
                try {
                    Model.firebase.register(email, password, username)
                    Toast.makeText(this@RegisterActivity, "Registered. Please login.", Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@RegisterActivity, e.message ?: "Register failed", Toast.LENGTH_SHORT).show()
                } finally {
                    setLoading(false)
                }
            }
        }

        binding.registerLink.setOnClickListener { finish() }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.loginBtn.isEnabled = !isLoading
    }
}
