package com.colman.booknook.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.colman.booknook.R
import com.colman.booknook.databinding.ActivityMainBinding
import com.colman.booknook.model.Model

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val navController = navHostFragment?.navController
        val bottomNav = binding.bottomNav

        if (navController != null) {
            androidx.navigation.ui.NavigationUI.setupWithNavController(bottomNav, navController)
        }

        if (Model.currentUserId() != null) {
             navController?.navigate(R.id.postsFragment)
        }
    }
}
