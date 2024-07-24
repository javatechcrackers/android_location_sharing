package com.example.geofencelive.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.geofencelive.UtilityClasses.FirestoreWorker
import com.example.geofencelive.databinding.ActivityLoginBinding
import com.example.geofencelive.databinding.ActivityMainBinding
import com.example.geofencelive.ui.theme.GeoFenceLiveTheme
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private var binding: ActivityMainBinding?= null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding?.root)

        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("userEmail", null)

        binding?.useremail?.text = userEmail





        binding?.logoutbutton?.setOnClickListener{
            logoutUser()
        }

        binding?.geofenceButton?.setOnClickListener{
            val intent = Intent(this, GeofenceMapsActivity::class.java)
            startActivity(intent)
        }

        binding?.viewGroupButton?.setOnClickListener{
            val intent = Intent(this, GroupActivity::class.java)
            startActivity(intent)
        }

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

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GeoFenceLiveTheme {
        Greeting("Android")
    }
}