package com.example.geofencelive.fragments

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.geofencelive.R
import com.example.geofencelive.UtilityClasses.FirestoreWorker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.SettingsClient
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.TimeUnit
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.location.Location
import android.util.Log
import android.widget.Toast
import com.example.geofencelive.activities.GeofenceMapsActivity
import com.example.geofencelive.activities.GroupActivity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.tasks.Task


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class HomeFragment : Fragment() {
//    private lateinit var btnLiveTracking: Button
//    private lateinit var btnShareCurrentLocation : Button
//    private lateinit var btnGeoFence: Button
//    private lateinit var fusedLocationClient: FusedLocationProviderClient
//    private lateinit var databaseReference: DatabaseReference
//    private val BACKGROUND_LOCATION_REQUEST_CODE = 10002
//    private lateinit var settingsClient: SettingsClient
//    private lateinit var geofencingClient: GeofencingClient
//    private  var userName : String? = null





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        val workRequest = PeriodicWorkRequestBuilder<FirestoreWorker>(15, TimeUnit.MINUTES).build()
//        WorkManager.getInstance(requireContext()).enqueue(workRequest)
//
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
//        databaseReference = FirebaseDatabase.getInstance().reference.child("locations")
//
//        geofencingClient = LocationServices.getGeofencingClient(requireContext())
//        settingsClient = LocationServices.getSettingsClient(requireContext())
//        val sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
//        userName = sharedPreferences.getString("displayName", null)


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)
//        btnLiveTracking = view.findViewById(R.id.fg_btn_live_tracking)
//        btnGeoFence = view.findViewById(R.id.fg_btn_geofence)
//        btnShareCurrentLocation = view.findViewById(R.id.fg_btn_share_current_location)
//
//        btnShareCurrentLocation.setOnClickListener {
//            shareCurrentLocation()
//        }

//        btnGeoFence.setOnClickListener {
//            if (ActivityCompat.checkSelfPermission(
//                    requireContext(),
//                    Manifest.permission.ACCESS_FINE_LOCATION
//                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
//                    requireContext(),
//                    Manifest.permission.ACCESS_COARSE_LOCATION
//                ) != PackageManager.PERMISSION_GRANTED &&  ActivityCompat.checkSelfPermission(
//                    requireContext(),
//                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
//            ) {
//                ActivityCompat.requestPermissions(
//                    requireActivity(),
//                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
//                    1
//                )
//
//                ActivityCompat.requestPermissions(
//                    requireActivity(),
//                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
//                    BACKGROUND_LOCATION_REQUEST_CODE
//                )
//            }
//
//            if (ActivityCompat.checkSelfPermission(
//                    requireContext(),
//                    Manifest.permission.ACCESS_FINE_LOCATION
//                ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
//                    requireContext(),
//                    Manifest.permission.ACCESS_COARSE_LOCATION
//                ) == PackageManager.PERMISSION_GRANTED &&  ActivityCompat.checkSelfPermission(
//                    requireContext(),
//                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
//            ) {
//                checkLocationSettings()
//            } else {
//                if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_BACKGROUND_LOCATION)){
//                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), BACKGROUND_LOCATION_REQUEST_CODE)
//                } else {
//                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), BACKGROUND_LOCATION_REQUEST_CODE)
//                }
//                Toast.makeText(requireContext(), "Please give location permission", Toast.LENGTH_LONG).show()
//            }
//        }
//
//




        return inflater.inflate(R.layout.fragment_home, container, false)
    }

//    private fun checkLocationSettings() {
//        val locationRequest = LocationRequest.create().apply {
//            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
//        }
//
//        val builder = LocationSettingsRequest.Builder()
//            .addLocationRequest(locationRequest)
//            .setAlwaysShow(true)
//
//        val locationSettingsRequest = builder.build()
//
//        val task: Task<LocationSettingsResponse> = settingsClient.checkLocationSettings(locationSettingsRequest)
//
//        task.addOnSuccessListener { locationSettingsResponse ->
//            val intent = Intent(requireContext(), GeofenceMapsActivity::class.java)
//            startActivity(intent)
//
//            // All location settings are satisfied. The client can initialize location requests here.
//            Log.d("LocationSettings", "All location settings are satisfied.")
//        }
//
//        task.addOnFailureListener { exception ->
//            if (exception is ResolvableApiException) {
//                // Location settings are not satisfied, but this can be fixed by showing the user a dialog.
//                try {
//                    exception.startResolutionForResult(requireActivity(), GroupActivity.REQUEST_CHECK_SETTINGS)
//                } catch (sendEx: IntentSender.SendIntentException) {
//                    // Ignore the error.
//                }
//            }
//        }
//    }
//
//    private fun shareCurrentLocation(){
//        if (ActivityCompat.checkSelfPermission(
//                requireContext(),
//                Manifest.permission.ACCESS_FINE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
//                requireContext(),
//                Manifest.permission.ACCESS_COARSE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            ActivityCompat.requestPermissions(
//                requireActivity(),
//                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
//                1
//            )
//            return
//        }
//        fusedLocationClient.lastLocation
//            .addOnSuccessListener { location: Location? ->
//                location?.let {
//                    // shareLocation(it.latitude, it.longitude)
//
//                    val latitude : Double = it.latitude
//                    val longitude : Double = it.longitude
//
//                    val uri = "http://maps.google.com/maps?q=$latitude,$longitude"
//                    val shareIntent = Intent().apply {
//                        action = Intent.ACTION_SEND
//                        putExtra(Intent.EXTRA_TEXT, "Check out my location: $uri")
//                        type = "text/plain"
//                    }
//                    startActivity(Intent.createChooser(shareIntent, "Share location via"))
//                }
//            }
//    }

    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}