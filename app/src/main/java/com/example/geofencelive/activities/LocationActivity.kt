package com.example.geofencelive.activities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.geofencelive.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LocationActivity : AppCompatActivity(), OnMapReadyCallback  {

    private lateinit var mMap: GoogleMap
    private lateinit var databaseReference: DatabaseReference
    private lateinit var oneToOneDatabaseReference: DatabaseReference
    private var userMarkers: MutableMap<String, Marker> = mutableMapOf()
    private var activeUserIds: MutableList<String> = mutableListOf()
    private var oneToOneUserIds: MutableList<String> = mutableListOf()
    private var familyMembers: MutableList<String> = mutableListOf()
    private lateinit var userId : String
    private lateinit var spinnerFamilyMembers: Spinner
    private lateinit var spinnerGroupMembers : Spinner
    private lateinit var btnAddRemoveMember: Button
    private lateinit var btnOneToOneLocation : Button
    private  var userName : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

//        userId = intent.getStringExtra("userId").toString()
//        activeUserIds.add(userId);
        activeUserIds =  intent.getStringArrayListExtra("activeUserIds")?.toMutableList() ?: mutableListOf()
        databaseReference = FirebaseDatabase.getInstance().reference.child("locations")
        oneToOneDatabaseReference = FirebaseDatabase.getInstance().reference.child("onetoonelocations")
        spinnerFamilyMembers = findViewById(R.id.spinnerFamilyMembers)
        spinnerGroupMembers = findViewById(R.id.spinnerGroupMembers)
        btnAddRemoveMember = findViewById(R.id.btnAddRemoveMember)
        btnOneToOneLocation = findViewById(R.id.btnOneToOneLocation)
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        userName = sharedPreferences.getString("displayName", null)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupUI()

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        listenForLocationUpdates();
    }

    private fun listenForLocationUpdates() {
        activeUserIds.forEach { userID ->
            val userLocationRef = databaseReference.child(userID)
            userLocationRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val latitude = dataSnapshot.child("latitude").getValue(Double::class.java)
                        val longitude = dataSnapshot.child("longitude").getValue(Double::class.java)
                        val accuracy = dataSnapshot.child("accuracy").getValue(Float::class.java)

                        val location = Location("").apply {
                            this.latitude = latitude ?: 0.0
                            this.longitude = longitude ?: 0.0
                            this.accuracy = accuracy ?: 0.0f
                        }
                        updateLocationOnMap(userID, location)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Error fetching location: ${databaseError.message}")
                }
            })
        }
    }

    private fun listenForOneToOneLocationUpdates() {
        oneToOneUserIds.forEach { userID ->
            val userLocationRef = databaseReference.child(userID)
            userLocationRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val latitude = dataSnapshot.child("latitude").getValue(Double::class.java)
                        val longitude = dataSnapshot.child("longitude").getValue(Double::class.java)
                        val accuracy = dataSnapshot.child("accuracy").getValue(Float::class.java)

                        val location = Location("").apply {
                            this.latitude = latitude ?: 0.0
                            this.longitude = longitude ?: 0.0
                            this.accuracy = accuracy ?: 0.0f
                        }
                        updateLocationOnMap(userID, location)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Error fetching location: ${databaseError.message}")
                }
            })
        }

    }
    private fun setupUI() {
        // Setup spinner with family members
//        fetchSharedUsers();
        fetchOneToOneSharedUsers();
        val familyMembers = intent.getStringArrayListExtra("activeUserIds")?.toMutableList() ?: mutableListOf()
        Log.e("family Members ", familyMembers.toString());
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, familyMembers)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFamilyMembers.adapter = adapter


        btnOneToOneLocation.setOnClickListener{
            activeUserIds.forEach { id ->
                removeMarker(id)
            }
            listenForOneToOneLocationUpdates()
        }

        btnAddRemoveMember.setOnClickListener {
            if(activeUserIds.size == 0){
                activeUserIds =  intent.getStringArrayListExtra("activeUserIds")?.toMutableList() ?: mutableListOf()
                listenForLocationUpdates()
            }else{
                val selectedUser = spinnerFamilyMembers.selectedItem.toString()
                if (activeUserIds.contains(selectedUser)) {
                    activeUserIds.remove(selectedUser)
                    removeMarker(selectedUser)
                } else {
                    activeUserIds.add(selectedUser)
                    listenForLocationUpdates()
                }
                if (activeUserIds.isEmpty()) {
                    Toast.makeText(this,"Add a member to trace!!",Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    private fun removeMarker(userId: String) {
        val marker = userMarkers[userId]
        marker?.remove()
        userMarkers.remove(userId)
        adjustZoom()
    }



    private fun updateLocationOnMap(userId: String?, location: Location) {
        val userLatLng = LatLng(location.latitude, location.longitude)

        // Check if the marker for this user already exists
        val marker = userMarkers[userId]

        if (marker == null) {
            // Add a new marker if it doesn't exist
            val markerOptions = MarkerOptions()
                .position(userLatLng)
                .icon(createCustomMarker(this, userId ?: ""))
//                .title(userId)
            val newMarker = mMap.addMarker(markerOptions)
            if (newMarker != null) {
                userMarkers[userId!!] = newMarker
            }
        } else {
            marker.position = userLatLng
        }
        adjustZoom()
    }

    private fun adjustZoom() {
        if (userMarkers.isNotEmpty()) {
            if (userMarkers.size == 1) {
                val userLatLng = userMarkers.values.first().position
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 17f))
            } else {
                val builder = LatLngBounds.Builder()
                userMarkers.values.forEach { marker ->
                    builder.include(marker.position)
                }
                val bounds = builder.build()
                val padding = 100 // Padding in pixels
                val cu = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                mMap.animateCamera(cu)
            }
        }
    }

    private fun createCustomMarker(context: Context, userName: String): BitmapDescriptor {
        val markerView = (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.custom_marker, null)
        val markerTextView = markerView.findViewById<TextView>(R.id.marker_text)
        markerTextView.text = userName

        markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        markerView.layout(0, 0, markerView.measuredWidth, markerView.measuredHeight)

        val bitmap = Bitmap.createBitmap(markerView.measuredWidth, markerView.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        markerView.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun fetchOneToOneSharedUsers() {
        // Access the current user's node in the database
        val currentUserRef = userName?.let { oneToOneDatabaseReference.child(it) }

        // Add a single value event listener to the current user's node
        if (currentUserRef != null) {
            currentUserRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    oneToOneUserIds.clear()

                    // Iterate through the children of the current user's node
                    dataSnapshot.children.forEach { snapshot ->
                        val userId = snapshot.key ?: return@forEach
                        Log.e("child userId :", userId.toString())

                        // Add the child userId to the sharedUsers list and userAdapter
                        oneToOneUserIds.add(userId)
                    }

                    // Log the list of shared users and notify the adapter of data changes
                    Log.e("Shared Users under $userName ::", oneToOneUserIds.toString())
//                    userAdapter.notifyDataSetChanged()
                    setAdapter();
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle possible errors
                    Log.e("Database Error", "Error fetching data: ${databaseError.message}")
                }
            })
        }
    }
    private fun setAdapter(){
        val groupAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, oneToOneUserIds)
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGroupMembers.adapter = groupAdapter
        Log.e("OneToOne : ", oneToOneUserIds.toString());
        spinnerGroupMembers.setSelection(0)
    }

}