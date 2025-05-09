package com.example.socialmedia

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.socialmedia.Fragments.HomeFragment
import com.example.socialmedia.Fragments.NotificationFragment
import com.example.socialmedia.Fragments.ProfileFragment
import com.example.socialmedia.Fragments.SearchFragment
import com.example.socialmedia.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    internal var selectedFragment: Fragment? = null

    private val onNavigationItemSelectedListener =
        BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    moveToFragment(HomeFragment())
                    return@OnNavigationItemSelectedListener true
                }

                R.id.nav_search -> {
                    moveToFragment(SearchFragment())
                    return@OnNavigationItemSelectedListener true
                }

                R.id.nav_addpost -> {
                    item.isChecked = false
                    startActivity(Intent(this@MainActivity, AddPostActivity::class.java))
                    return@OnNavigationItemSelectedListener true
                }

                R.id.nav_notifications -> {
                    moveToFragment(NotificationFragment())
//                    setContent {
//                        NotificationFragment()
//                    }
                    return@OnNavigationItemSelectedListener true
                }

                R.id.nav_profile -> {
                    moveToFragment(ProfileFragment())
                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(findViewById(R.id.home_toolbar))

        binding.navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        val publisher = intent.getStringExtra("PUBLISHER_ID")
        if (publisher != null) {
            val prefs = getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
            prefs.putString("profileId", publisher)
            prefs.apply()

            moveToFragment(ProfileFragment())
        } else {
            moveToFragment(HomeFragment())
        }
    }

    private fun moveToFragment(fragment: Fragment) {
        selectedFragment = fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
