package com.example.geofencelive.activities

import android.graphics.Canvas
import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.geofencelive.BuildConfig
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest

import com.example.geofencelive.BuildConfig.GEOFENCE_MAPS_API_KEY
import com.example.geofencelive.R
import com.example.geofencelive.UtilityClasses.GeofenceHelper

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.geofencelive.databinding.ActivityGeofenceMapsBinding
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.libraries.places.api.Places
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import com.example.geofencelive.UtilityClasses.FirestoreHelper
import com.example.geofencelive.UtilityClasses.NotificationHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker

class GeofenceMapsActivity : AppCompatActivity(), OnMapReadyCallback , GoogleMap.OnMapLongClickListener{

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityGeofenceMapsBinding

    private val TAG = "GEOFENCEMAPSACTIVITY"
    private var GEOFENCE_RADIUS = 500.0;
    private var durationMinutes = 2;
    private val FINE_LOCATION_ACCESS_REQUEST_CODE = 10001;
    private val BACKGROUND_LOCATION_REQUEST_CODE = 10002;


    lateinit var geofencingClient: GeofencingClient
    private lateinit var geofencingHelper: GeofenceHelper
    private lateinit var coordinatesToShow: LatLng


    private val geofenceList = mutableListOf<Geofence>()

    private lateinit var firestoreHelper : FirestoreHelper

    private val geofenceId = "Geofence_ID"
    private lateinit var currUserEmail : String
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocationMarker: Marker? = null
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback



    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGeofenceMapsBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
       // val editor = sharedPreferences.edit()
      //  editor.apply()


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        currUserEmail = sharedPreferences.getString("userEmail", null).toString()

        geofencingClient = LocationServices.getGeofencingClient(this)
        geofencingHelper = GeofenceHelper(this);
        firestoreHelper = FirestoreHelper()

        Places.initialize(applicationContext, BuildConfig.GEOFENCE_MAPS_API_KEY)

        val nlatitude = intent.getDoubleExtra("notificationLatitude", 28.4916)
        val nlongitude = intent.getDoubleExtra("notificationLongitude", 77.0745)

        coordinatesToShow = LatLng(nlatitude, nlongitude)

        binding?.tvgeofenceTime?.text = "$durationMinutes Minutes"

        binding?.geofenceEntryDurationMinus?.setOnClickListener {
            if(durationMinutes > 1){
                durationMinutes = durationMinutes-1;
                binding?.tvgeofenceTime?.text = "$durationMinutes Minutes"
            }
            else{
                Toast.makeText(this, "Time can not be less than 1 minute", Toast.LENGTH_SHORT).show()
            }
        }

        binding?.geofenceEntryDurationPlus?.setOnClickListener {
            if(durationMinutes < 60){
                durationMinutes = durationMinutes+1;
                binding?.tvgeofenceTime?.text = "$durationMinutes Minutes"
            }
            else{
                Toast.makeText(this, "Time can not be greater than 60 minutes", Toast.LENGTH_SHORT).show()
            }
        }

        binding?.geofenceRadiusMinus?.setOnClickListener {

            if(GEOFENCE_RADIUS > 100){
                GEOFENCE_RADIUS = GEOFENCE_RADIUS - 50;
                binding?.tvgeofenceRadius?.text = "$GEOFENCE_RADIUS metre"
            }
            else{
                Toast.makeText(this, "Radius can not be smaller than 100 metre", Toast.LENGTH_SHORT).show()
            }


        }

        binding?.geofenceRadiusPlus?.setOnClickListener {

            if(GEOFENCE_RADIUS<1000){
                GEOFENCE_RADIUS = GEOFENCE_RADIUS + 50;
                binding?.tvgeofenceRadius?.text = "$GEOFENCE_RADIUS metre"
            }
            else{
                Toast.makeText(this, "Radius can not be greater than 1000 metre", Toast.LENGTH_SHORT).show()
            }


        }

        binding?.tvgeofenceRadius?.text = "$GEOFENCE_RADIUS metre"

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupLocationUpdates()

    }

    override fun onResumeFragments() {
        super.onResumeFragments()

        val intent = intent
        if (Intent.ACTION_VIEW == intent.action) {
            val uri = intent.data
            uri?.let {
                val latitude = it.getQueryParameter("q")?.split(",")?.get(0)?.toDoubleOrNull()
                val longitude = it.getQueryParameter("q")?.split(",")?.get(1)?.toDoubleOrNull()
                if (latitude != null && longitude != null) {
                    val nlatLng : LatLng = LatLng(latitude, longitude);
                    // Use latitude and longitude
                    mMap.addMarker(MarkerOptions().position(nlatLng).title("User Current Location"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nlatLng, 16F))


                    Log.d("Geofence Activity", "Location: $latitude, $longitude")
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.S)
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val nagarro = LatLng(28.4916, 77.0745 )

        if(coordinatesToShow != nagarro){
            mMap.addMarker(MarkerOptions().position(coordinatesToShow).title("User coordinates when geofence event occur"))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinatesToShow, 16F))
            addMarker(coordinatesToShow)
        }

        else{
            mMap.addMarker(MarkerOptions().position(nagarro).title("Default Location"))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nagarro, 16F))
            addMarker(nagarro)
        }


        mMap.setOnMapLongClickListener(this)
        enableUserLocation();


        markPreviouslyAddedGeofences();
        //setupLocation()

        setupLocationUpdates()

    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setupLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).apply {
            setMinUpdateIntervalMillis(2000) // 5 seconds
        }.build()







        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    if (currentLocationMarker == null) {
                        currentLocationMarker = mMap.addMarker(
                            MarkerOptions().position(latLng)
                                .icon(BitmapDescriptorFactory.fromBitmap(getCustomLocationIcon()!!))
                                .title("You are here")
                        )
                    } else {
                        currentLocationMarker?.position = latLng
                    }
                  //  mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }


    private fun setupLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            // Got last known location. In some rare situations this can be null.
            location?.let {
                val latLng = LatLng(location.latitude, location.longitude)
                if (currentLocationMarker == null) {
                    currentLocationMarker = mMap.addMarker(MarkerOptions().position(latLng)
                        .icon(BitmapDescriptorFactory.fromBitmap(getCustomLocationIcon()!!))
                        .title("You are here"))
                } else {
                    currentLocationMarker?.position = latLng
                }
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            }
        }



    }

    private fun markPreviouslyAddedGeofences(){

        mMap.clear()
        firestoreHelper.getGeofenceList(currUserEmail) { geofences ->
            // Handle the geofences list here
            for (geofence in geofences) {
                Log.d("Happy", geofence.geofenceId)
                val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.apply()
                val key = "${geofence.geofenceId}${geofence.userEmail}"

                val isGeofenceExist = sharedPreferences.getString(key, null)
                val latLng = geofence.latLng;
                val radius = geofence.geofenceRadius

                if(isGeofenceExist != null){
                    addMarker(latLng)
                    addCircle(latLng, radius)
                    Log.d(TAG, geofence.userEmail)
                }else{
                    addGeofence(latLng,radius)
                    addMarker(latLng)
                    addCircle(latLng, radius)
                }


            }
        }
    }

    private fun getCustomLocationIcon(): Bitmap? {
        val vectorDrawable = ContextCompat.getDrawable(this, R.drawable.ic_custom_location)!!
        val h = vectorDrawable.intrinsicHeight
        val w = vectorDrawable.intrinsicWidth
        vectorDrawable.setBounds(0, 0, w, h)
        val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bm)
        vectorDrawable.draw(canvas)
        return bm
    }



    private fun addMarker(latLng: LatLng) {
        val markerOptions = MarkerOptions().position(latLng)

        mMap.addMarker(markerOptions)
    }

    private fun addCircle(latLng: LatLng, radius: Number) {
        val circleOptions = CircleOptions()
        circleOptions.center(latLng)
        circleOptions.radius(radius.toDouble())
        circleOptions.strokeColor(Color.argb(255, 0, 255, 0))
        circleOptions.fillColor(Color.argb(64, 0, 255, 0))
        circleOptions.strokeWidth(4f)
        mMap.addCircle(circleOptions)
    }

    private fun enableUserLocation(){
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            mMap.setMyLocationEnabled(true)
        }else{
            //ask for permission

            if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)){

                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), FINE_LOCATION_ACCESS_REQUEST_CODE)
            }else{
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), FINE_LOCATION_ACCESS_REQUEST_CODE)
            }
        }
    }
    private fun addGeofence(latLng: LatLng, radius: Double){
        val id = System.currentTimeMillis()
        val geofence: Geofence = geofencingHelper.getGeofence(id.toString(), latLng, radius, Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
        val geofencingRequest : GeofencingRequest = geofencingHelper.getGeofencingRequest(geofence)
        val pendingIntent : PendingIntent = geofencingHelper.getPendingIntent()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
            .addOnSuccessListener { Log.d(TAG, "onSuccess: Geofence Added...")
                geofenceList.add(geofence)
                val entryDeadline = System.currentTimeMillis() + durationMinutes * 60 * 1000 // current time + duration in milliseconds
                firestoreHelper.addGeofenceData(id.toString(), GEOFENCE_RADIUS, latLng, currUserEmail, entryDeadline)
                val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.apply()

                val key = "$id$currUserEmail"
                editor.putString(key, key) // Save device identifier if needed
                editor.apply()

                object : CountDownTimer((durationMinutes *60 *1000).toLong(), 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        // Do something every second if needed
                    }

                    override fun onFinish() {

                        var enteredFlag = false;

                        firestoreHelper.getGeofenceById(id.toString()){geofenceDoc->

                            enteredFlag = geofenceDoc?.enteredFlag!!

                            Log.d("Maps activity entered flag", enteredFlag.toString())

                            if(!enteredFlag){

                                firestoreHelper.postTransitionEvents("No Event Happened", latLng, currUserEmail)
//                            val notificationHelper = NotificationHelper(baseContext)
//                            notificationHelper.sendHighPriorityNotification(
//                                "No Transition Happened", "User $currUserEmail didn't entered into geofence in given time!!",
//                                GeofenceMapsActivity::class.java,latLng
//                            )

                            }

                        }

                    //   val entered =  firestoreHelper.getGeofencebyBool(id.toString())



                        // Timer finished, check if user entered the geofence
                        //  sendNotification(context, "No entry", "User did not enter the geofence within the specified time.")
                    }
                }.start()

            }
            .addOnFailureListener { e ->
                val errorMessage: String = geofencingHelper.getErrorString(e)
                Log.d(TAG, "onFailure: $errorMessage")
            }




    }

    private fun zoomOnMap(latLng:LatLng){
        val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(latLng, 16f)
        mMap?.animateCamera(newLatLngZoom)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == FINE_LOCATION_ACCESS_REQUEST_CODE){
            if(grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                // we have the permission
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {

                    return
                }
                mMap.setMyLocationEnabled(true)
            }else{
                // we don't have the permission
            }
        }

        if(requestCode == BACKGROUND_LOCATION_REQUEST_CODE){
            if(grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                // we have the permission

                //mMap.setMyLocationEnabled(true)
                Toast.makeText(this, "You can add geofences", Toast.LENGTH_SHORT).show()
            }else{
                // we don't have the permission
                Toast.makeText(this, "Background location access required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapLongClick(latLng: LatLng) {

        if(Build.VERSION.SDK_INT >= 29){
            if(ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED)
            {
               // mMap.clear()
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16F))
                addMarker(latLng)
                addCircle(latLng, GEOFENCE_RADIUS)
                zoomOnMap(latLng)
                addGeofence(latLng, GEOFENCE_RADIUS)
            }else{
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)){
                    ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION), BACKGROUND_LOCATION_REQUEST_CODE)
                }else{
                    ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION), BACKGROUND_LOCATION_REQUEST_CODE)
                }
            }
        }
        else{
           // mMap.clear()
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16F))
            addMarker(latLng)
            addCircle(latLng, GEOFENCE_RADIUS)
            zoomOnMap(latLng)
            addGeofence(latLng, GEOFENCE_RADIUS)
        }


    }
}