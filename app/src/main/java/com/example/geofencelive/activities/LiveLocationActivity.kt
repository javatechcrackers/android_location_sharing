package com.example.geofencelive.activities

import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.os.Bundle
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.location.Location
import android.os.Looper
import android.util.Log
import com.example.geofencelive.R
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*
import java.util.UUID

class LiveLocationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var databaseReference: DatabaseReference
    private lateinit var mMap: GoogleMap
    private lateinit var userId: String;
    private var userMarkers: MutableMap<String, Marker> = mutableMapOf() // To keep track of markers

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the map fragment
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

//          userId = getOrCreateUserId()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        databaseReference = FirebaseDatabase.getInstance().reference.child("locations")

        // Request location permissions if not already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            requestLocationUpdates()
        }
        listenForLocationUpdates()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
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
                    sendLocationToFirebase(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun sendLocationToFirebase(location: Location) {
        Log.d("Firebase", "Sending location: $location")
        val locationData = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "accuracy" to location.accuracy, // Assuming accuracy is a field in Location class
            // Add other fields as needed
        )
//          databaseReference.child(userId).setValue(locationData)
        databaseReference.child("54c43756-341c-4b0a-9f1b-988fc5d84064").setValue(locationData)
    }

    private fun listenForLocationUpdates() {
        // Specify the user ID you want to listen to
        val specificUserId = "ed9ba936-63b5-4e35-88fb-e926a5739c8a"

        // Reference to the specific user's location in the database
        val userLocationRef = databaseReference.child(specificUserId)

        // Add a ValueEventListener to the specific user's location reference
        userLocationRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Check if there is data for the specific user
                if (dataSnapshot.exists()) {
                    val latitude = dataSnapshot.child("latitude").getValue(Double::class.java)
                    val longitude = dataSnapshot.child("longitude").getValue(Double::class.java)
                    val accuracy = dataSnapshot.child("accuracy").getValue(Float::class.java)

                    // Create a Location object
                    val location = Location("").apply {
                        this.latitude = latitude ?: 0.0
                        this.longitude = longitude ?: 0.0
                        this.accuracy = accuracy ?: 0.0f
                    }

                    // Update the map with the location data for the specific user
                    updateLocationOnMap(specificUserId, location)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle possible errors
                Log.e("Firebase", "Error fetching location: ${databaseError.message}")
            }
        })
    }



    private fun updateLocationOnMap(userId: String?, location: Location) {
        val userLatLng = LatLng(location.latitude, location.longitude)

        // Check if the marker for this user already exists
        val marker = userMarkers[userId]

        if (marker == null) {
            // Add a new marker if it doesn't exist
            val markerOptions = MarkerOptions().position(userLatLng).title(userId)
            val newMarker = mMap.addMarker(markerOptions)
            if (newMarker != null) {
                userMarkers[userId!!] = newMarker
            }
        } else {
            // Update the position of the existing marker
            marker.position = userLatLng
        }

        // Optionally, move the camera to the user's location
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
    }

    private fun getOrCreateUserId(): String {
        val sharedPreferences = getSharedPreferences("com.example.locationsharingdemo", Context.MODE_PRIVATE)
        var userId = sharedPreferences.getString("userId", null)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
        if (userId == null) {
            userId = UUID.randomUUID().toString()
            sharedPreferences.edit().putString("userId", userId).apply()
        }
        return userId
    }


}