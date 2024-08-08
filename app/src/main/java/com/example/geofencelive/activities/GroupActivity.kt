package com.example.geofencelive.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.geofencelive.BroadcastReceivers.GeofenceBroadcastReceiver
import com.example.geofencelive.Models.GeofenceTransitionModel
import com.example.geofencelive.R
import com.example.geofencelive.UtilityClasses.FirestoreWorker
import com.example.geofencelive.UtilityClasses.NotificationAdapter
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.concurrent.TimeUnit

class GroupActivity : AppCompatActivity() {

    val layoutId = R.layout.activity_group

    private lateinit var btnLiveTracking: ImageButton
    private lateinit var layoutLiveTrackingOptions: LinearLayout
    private lateinit var btnShareLocation: Button
    private lateinit var btnSharedLocation: Button
    private lateinit var btnLogout : ImageButton
    private lateinit var spinnerSharedLocations: Spinner
    private lateinit var btnShareCurrentLocation : ImageButton
    private lateinit var btnGeoFence: ImageButton
    private lateinit var nameText : TextView
    private  var userName : String? = null
    private lateinit var listGeoFenceUsers: ListView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var databaseReference: DatabaseReference
    private lateinit var userAdapter: ArrayAdapter<String>
    private val sharedUsers = mutableListOf<String>()
    private var isUserInteractingWithSpinner = false
    private val BACKGROUND_LOCATION_REQUEST_CODE = 10002;
    private lateinit var settingsClient: SettingsClient

    private lateinit var recyclerView: RecyclerView
    private lateinit var geofenceEventAdapter: NotificationAdapter
    private lateinit var geofenceEventList: MutableList<GeofenceTransitionModel>
    private val db = FirebaseFirestore.getInstance()




    lateinit var geofencingClient: GeofencingClient

    @SuppressLint("MissingInflatedId", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group)

        val workRequest = PeriodicWorkRequestBuilder<FirestoreWorker>(15, TimeUnit.MINUTES)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.statusbar)

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }

        WorkManager.getInstance(this).enqueue(workRequest)

        recyclerView = findViewById(R.id.notification_recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        geofenceEventList = mutableListOf()
        geofenceEventAdapter = NotificationAdapter(geofenceEventList)
        recyclerView.adapter = geofenceEventAdapter

        fetchGeofenceEvents()

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

        geofencingClient = LocationServices.getGeofencingClient(this)
        settingsClient = LocationServices.getSettingsClient(this)
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        userName = sharedPreferences.getString("displayName", null)

        nameText.text = "Mr $userName"

        val userImg : de.hdodenhof.circleimageview.CircleImageView  = findViewById(R.id.user_circular_image_view)

        if(userName == "Ashutosh Pandey"){
            userImg.setImageResource(R.drawable.media_ashutosh)
        }
        else if(userName == "Prashant Katiyar"){
            userImg.setImageResource(R.drawable.media_prashant)
        }else if(userName == "Harish Kumar"){
            userImg.setImageResource(R.drawable.media_harish)
        }else{
            userImg.setImageResource(R.drawable.media_rohit)
        }

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
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&  ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    1
                )

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    BACKGROUND_LOCATION_REQUEST_CODE
                )

            }

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED &&  ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {

               // val intent = Intent(this, GeofenceMapsActivity::class.java)
              //  startActivity(intent)

                checkLocationSettings()

            }else{
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)){
                    ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION), BACKGROUND_LOCATION_REQUEST_CODE)
                }else{
                    ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION), BACKGROUND_LOCATION_REQUEST_CODE)
                }
                Toast.makeText(this, "Please give location permission", Toast.LENGTH_LONG).show()
            }



        }
    }

    private fun fetchGeofenceEvents(){
        db.collection("geofence")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val useremail = document.getString("useremail")
                    val transition = document.getString("transitionType")
                    val time = document.getLong("createdAt")
                    val latLngMap = document.get("latLng") as? Map<String, Any>
                    val latitude = latLngMap?.get("latitude") as? Double
                    val longitude = latLngMap?.get("longitude") as? Double

                    val currCoordinates = LatLng(latitude!!, longitude!!)

                    val geofenceEvent : GeofenceTransitionModel = GeofenceTransitionModel(
                            transition,
                            currCoordinates,
                            useremail!!,
                            time!!
                            )
                   // val geofenceEvent = document.toObject(GeofenceTransitionModel::class.java)
                    geofenceEventList.add(geofenceEvent)
                }
                geofenceEventAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.w("GroupActivity", "Error getting documents: ", exception)
            }
    }

    private fun removeAllGeofences(context: Context) {
        geofencingClient.removeGeofences(geofencePendingIntent)?.run {
            addOnSuccessListener {
                // Geofences removed successfully
                Log.d("Geofence Removed  ", "Geofences removed successfully")

                val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.clear() // Clears all data from SharedPreferences
                editor.apply() // Apply changes asynchronously

                // Optional: Redirect user to the login screen or any other appropriate activity
                val intent = Intent(context, LoginActivity::class.java)
                startActivity(intent)
                finish() // Close current activity
            }
            addOnFailureListener {
                // Failed to remove geofences
                Log.d("Geofence Removed  ", "Failed to remove geofences")
            }
        }
    }

    private val geofencePendingIntent: PendingIntent by lazy {

        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            this,
            2607,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )


       // val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        //PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun shareCurrentLocation(){
        Toast.makeText(this, "Clicked", Toast.LENGTH_LONG).show()
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

        removeAllGeofences(this)
        // Clear SharedPreferences data
//        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
//        val editor = sharedPreferences.edit()
//        editor.clear() // Clears all data from SharedPreferences
//        editor.apply() // Apply changes asynchronously
//
//        // Optional: Redirect user to the login screen or any other appropriate activity
//        val intent = Intent(this, LoginActivity::class.java)
//        startActivity(intent)
//        finish() // Close current activity
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

    private fun checkLocationSettings() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val locationSettingsRequest = builder.build()

        val task: Task<LocationSettingsResponse> = settingsClient.checkLocationSettings(locationSettingsRequest)

        task.addOnSuccessListener { locationSettingsResponse ->
            val intent = Intent(this, GeofenceMapsActivity::class.java)
            startActivity(intent)

            // All location settings are satisfied. The client can initialize location requests here.
            Log.d("LocationSettings", "All location settings are satisfied.")
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed by showing the user a dialog.
                try {
                    exception.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                val intent = Intent(this, GeofenceMapsActivity::class.java)
                 startActivity(intent)
                // Location settings changed successfully.
                Log.d("LocationSettings", "User enabled location settings.")
            } else {
                // User chose not to make required location settings changes.
                Log.d("LocationSettings", "User did not enable location settings.")
            }
        }
    }

    companion object {
       // @LayoutRes
       // const val layoutId = R.layout.activity_group

        const val REQUEST_CHECK_SETTINGS = 100
    }
}