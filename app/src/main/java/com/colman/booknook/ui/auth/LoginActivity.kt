package com.booknook.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.booknook.app.databinding.ActivityLoginBinding
import com.booknook.app.model.Model
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Auto-login
        if (Model.currentUserId() != null) {
            startActivity(Intent(this, com.booknook.app.ui.main.MainActivity::class.java))
            finish()
            return
        }

        binding.loginBtn.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email and password required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            setLoading(true)
            lifecycleScope.launch {
                try {
                    Model.firebase.login(email, password)
                    startActivity(Intent(this@LoginActivity, com.booknook.app.ui.main.MainActivity::class.java))
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, e.message ?: "Login failed", Toast.LENGTH_SHORT).show()
                } finally {
                    setLoading(false)
                }
            }
        }

        binding.registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.loginBtn.isEnabled = !isLoading
    }
}
