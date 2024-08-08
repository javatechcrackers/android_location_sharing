package com.example.geofencelive.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.geofencelive.R

class WelcomeActivity : AppCompatActivity() {
    private lateinit var nextButton: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        nextButton = findViewById<RelativeLayout>(R.id.btnNext).findViewById(R.id.btn)

        nextButton.text = "Next"

        nextButton.setOnClickListener{
            val intent = Intent(this, CreateProfileActivity::class.java)
            startActivity(intent)
            finish();

        }


        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.apply()

        val userEmail = sharedPreferences.getString("userEmail", null)
        if(!userEmail.isNullOrEmpty()){
            // redirectToMain()
            val intent = Intent(this, GroupActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}