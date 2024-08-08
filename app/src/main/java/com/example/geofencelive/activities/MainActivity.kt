package com.example.geofencelive.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity

import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.geofencelive.R
import com.example.geofencelive.UtilityClasses.FirestoreWorker
import com.example.geofencelive.databinding.ActivityMainBinding
import com.example.geofencelive.fragments.HomeFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.concurrent.TimeUnit
import androidx.fragment.app.Fragment
import com.example.geofencelive.fragments.MessageFragment

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding?= null
    private lateinit var bottom_navigation : BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding?.root)

        bottom_navigation = binding?.bottomNavigationView!!

        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("userEmail", null)

//        if (savedInstanceState == null) {
//            supportFragmentManager.beginTransaction().replace(R.id.fragment_container, HomeFragment()).commit()
//        }

        binding?.bottomNavigationView!!.setOnItemSelectedListener { item ->

            when(item.itemId){
                R.id.bottom_nav_home ->{
                    supportFragmentManager.beginTransaction().replace(R.id.fragment_container, HomeFragment()).commit()
                    true
                }

                R.id.bottom_nav_message->{
                    supportFragmentManager.beginTransaction().replace(R.id.fragment_container, MessageFragment()).commit()
                    true
                }

                else -> {
                   false
                }
            }



        }



       // binding?.useremail?.text = userEmail




//
//        binding?.logoutbutton?.setOnClickListener{
//            logoutUser()
//        }
//
//        binding?.geofenceButton?.setOnClickListener{
//            val intent = Intent(this, GeofenceMapsActivity::class.java)
//            startActivity(intent)
//        }
//
//        binding?.viewGroupButton?.setOnClickListener{
//            val intent = Intent(this, GroupActivity::class.java)
//            startActivity(intent)
//        }

        val workRequest = PeriodicWorkRequestBuilder<FirestoreWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }



    private fun logoutUser() {
        // Clear SharedPreferences data
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear() // Clears all data from SharedPreferences
        editor.apply() // Apply changes asynchronously

        // Optional: Redirect user to the login screen or any other appropriate activity
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Close current activity
    }





}

