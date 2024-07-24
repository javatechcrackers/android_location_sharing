package com.example.geofencelive.activities

import android.Manifest
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.geofencelive.BuildConfig
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

class GeofenceMapsActivity : AppCompatActivity(), OnMapReadyCallback , GoogleMap.OnMapLongClickListener{

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityGeofenceMapsBinding

    private val TAG = "GEOFENCEMAPSACTIVITY"
    private val GEOFENCE_RADIUS = 500.0;
    private val FINE_LOCATION_ACCESS_REQUEST_CODE = 10001;
    private val BACKGROUND_LOCATION_REQUEST_CODE = 10002;

    lateinit var geofencingClient: GeofencingClient
    private lateinit var geofencingHelper: GeofenceHelper
    private lateinit var coordinatesToShow: LatLng

    private val geofenceId = "Geofence_ID"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGeofenceMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        geofencingClient = LocationServices.getGeofencingClient(this)
        geofencingHelper = GeofenceHelper(this);

        Places.initialize(applicationContext, BuildConfig.GEOFENCE_MAPS_API_KEY)

        val nlatitude = intent.getDoubleExtra("notificationLatitude", 28.4916)
        val nlongitude = intent.getDoubleExtra("notificationLongitude", 77.0745)

        coordinatesToShow = LatLng(nlatitude, nlongitude)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val nagarro = LatLng(28.4916, 77.0745 )

        if(coordinatesToShow != nagarro){
            mMap.addMarker(MarkerOptions().position(coordinatesToShow).title("User coordinates when geofence event occur"))
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinatesToShow, 16F))
        addMarker(coordinatesToShow)
        mMap.setOnMapLongClickListener(this)
        enableUserLocation();

        // Add a marker in Sydney and move the camera
//        val sydney = LatLng(-34.0, 151.0)
//        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }

    private fun addMarker(latLng: LatLng) {
        val markerOptions = MarkerOptions().position(latLng)
        mMap.addMarker(markerOptions)
    }

    private fun addCircle(latLng: LatLng, radius: Number) {
        val circleOptions = CircleOptions()
        circleOptions.center(latLng)
        circleOptions.radius(radius.toDouble())
        circleOptions.strokeColor(Color.argb(255, 255, 0, 0))
        circleOptions.fillColor(Color.argb(64, 255, 0, 0))
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
        val geofence: Geofence = geofencingHelper.getGeofence(geofenceId, latLng, radius, Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
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
            .addOnSuccessListener { Log.d(TAG, "onSuccess: Geofence Added...") }
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
                mMap.clear()
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
            mMap.clear()
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16F))
            addMarker(latLng)
            addCircle(latLng, GEOFENCE_RADIUS)
            zoomOnMap(latLng)
            addGeofence(latLng, GEOFENCE_RADIUS)
        }


    }
}