package com.example.socialmedia

import android.R
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.socialmedia.databinding.ActivitySplashScreenBinding // Import the binding class

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding // Declare the binding variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater) // Inflate the layout using binding
        setContentView(binding.root) // Set the content view to the root of the binding

        binding.splashscreen.alpha = 0f // Use binding
        binding.splashscreen.animate().setDuration(2200).alpha(1f).withEndAction { // Use binding
            val i = Intent(this, LoginActivity::class.java)
            startActivity(i)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }
    }
}