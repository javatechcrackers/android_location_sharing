package com.example.geofencelive.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.geofencelive.R
import com.example.geofencelive.UtilityClasses.FirestoreWorker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.TimeUnit

class GroupActivity : AppCompatActivity() {
    private lateinit var btnLiveTracking: Button
    private lateinit var layoutLiveTrackingOptions: LinearLayout
    private lateinit var btnShareLocation: Button
    private lateinit var btnSharedLocation: Button
    private lateinit var btnLogout : Button
    private lateinit var spinnerSharedLocations: Spinner
    private lateinit var btnShareCurrentLocation : Button
    private lateinit var btnGeoFence: Button
    private lateinit var nameText : TextView
    private  var userName : String? = null
    private lateinit var listGeoFenceUsers: ListView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var databaseReference: DatabaseReference
    private lateinit var userAdapter: ArrayAdapter<String>
    private val sharedUsers = mutableListOf<String>()
    private var isUserInteractingWithSpinner = false

    @SuppressLint("MissingInflatedId", "ClickableViewAccessibility")
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
        spinnerSharedLocations = findViewById(R.id.spinnerSharedLocations)
        btnLogout = findViewById(R.id.btn_user_logout)
        btnGeoFence = findViewById(R.id.btn_geo_fence)
        listGeoFenceUsers = findViewById(R.id.list_geo_fence_users)
        nameText = findViewById(R.id.tv_greetings)
        btnShareCurrentLocation = findViewById(R.id.btn_share_current_location)

        // Set the flag when the user starts interacting with the spinner
        spinnerSharedLocations.setOnTouchListener { v, event ->
            isUserInteractingWithSpinner = true
            false
        }

        userAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSharedLocations.adapter = userAdapter

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
            fetchSharedUsers()
        }

        btnShareCurrentLocation.setOnClickListener {
            shareCurrentLocation()
        }

        spinnerSharedLocations.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (isUserInteractingWithSpinner  ) {
                    // Handle the selection as usual
                    if (position < sharedUsers.size) {
                        val selectedUser = sharedUsers[position]
                        val intent = Intent(this@GroupActivity, LocationActivity::class.java)
                        intent.putExtra("userId", selectedUser)
                        intent.putStringArrayListExtra("activeUserIds", ArrayList(sharedUsers))
                        startActivity(intent)
                    } else {
                        Log.e("SpinnerSelection", "Selected position $position out of bounds for sharedUsers list of size ${sharedUsers.size}")
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        btnLogout.setOnClickListener {
            logoutUser()
        }

        btnGeoFence.setOnClickListener {
            val intent = Intent(this, GeofenceMapsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun shareCurrentLocation(){
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1
            )
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                   // shareLocation(it.latitude, it.longitude)

                    val latitude : Double = it.latitude
                    val longitude : Double = it.longitude

                    val uri = "http://maps.google.com/maps?q=$latitude,$longitude"
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Check out my location: $uri")
                        type = "text/plain"
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share location via"))
                }
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
    private fun fetchSharedUsers() {
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                sharedUsers.clear()
                userAdapter.clear()
                dataSnapshot.children.forEach { snapshot ->
                    val userId = snapshot.key ?: return@forEach
                    sharedUsers.add(userId);
                    userAdapter.add(userId)
                }
                userAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle possible errors
            }
        })
    }
}