package com.example.geofencelive.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.geofencelive.R
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.geofencelive.UtilityClasses.FirestoreWorker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.TimeUnit

class GroupActivity : AppCompatActivity() {
    private lateinit var btnLiveTracking: Button
    private lateinit var layoutLiveTrackingOptions: LinearLayout
    private lateinit var btnShareLocation: Button
    private lateinit var btnSharedLocation: Button
    private lateinit var btnLogout : Button
    private lateinit var spinnerSharedLocations: Spinner
    private lateinit var btnGeoFence: Button
    private lateinit var nameText : TextView
    private  var userName : String? = null
    private lateinit var listGeoFenceUsers: ListView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var databaseReference: DatabaseReference

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group)

        val workRequest = PeriodicWorkRequestBuilder<FirestoreWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)

        btnLiveTracking = findViewById(R.id.btn_live_tracking)
        layoutLiveTrackingOptions = findViewById(R.id.layout_live_tracking_options)
        btnShareLocation = findViewById(R.id.btn_share_location)
        btnSharedLocation = findViewById(R.id.btn_shared_location)
        btnLogout = findViewById(R.id.btn_user_logout)
        spinnerSharedLocations = findViewById(R.id.spinner_shared_locations)
        btnGeoFence = findViewById(R.id.btn_geo_fence)
        listGeoFenceUsers = findViewById(R.id.list_geo_fence_users)
        nameText = findViewById(R.id.tv_greetings)


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        databaseReference = FirebaseDatabase.getInstance().reference.child("locations")

        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        userName = sharedPreferences.getString("displayName", null)

        nameText.text = "Hi $userName"

        btnLiveTracking.setOnClickListener {
            toggleVisibility(layoutLiveTrackingOptions)
            listGeoFenceUsers.visibility = View.GONE
        }

        btnShareLocation.setOnClickListener {
            showShareLocationConfirmationDialog()
        }

        btnSharedLocation.setOnClickListener {
            toggleVisibility(spinnerSharedLocations)
        }

        btnLogout.setOnClickListener {
            logoutUser()
        }

        btnGeoFence.setOnClickListener {
//            toggleVisibility(listGeoFenceUsers)
//            layoutLiveTrackingOptions.visibility = View.GONE

            val intent = Intent(this, GeofenceMapsActivity::class.java)
            startActivity(intent)
        }
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

    private fun toggleVisibility(view: View) {
        view.visibility = if (view.visibility == View.GONE) View.VISIBLE else View.GONE
    }

    private fun showShareLocationConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Share Location")
        builder.setMessage("Your live location will be shared with group members. Are you okay with sharing your location?")

        builder.setPositiveButton("Yes") { dialog, _ ->
            dialog.dismiss()
            shareLiveLocation()
        }

        builder.setNegativeButton("No") { dialog, _ ->
            Toast.makeText(this, "Location sharing canceled", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }
    private fun showLocationSharedDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Location Shared")
        builder.setMessage("Your location is now visible to other group members.")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun shareLiveLocation(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            requestLocationUpdates()
            showLocationSharedDialog()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000 // 5 seconds
            fastestInterval = 2000 // 2 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    sendLocationToFirebase(location);
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun sendLocationToFirebase(location: Location) {
        val locationData = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "accuracy" to location.accuracy,
        )
        userName?.let { databaseReference.child(it).setValue(locationData) }

    }
}