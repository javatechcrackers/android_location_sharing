package com.example.geofencelive.BroadcastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.util.Log
import android.widget.Toast

import com.example.geofencelive.UtilityClasses.FirestoreHelper
import com.example.geofencelive.UtilityClasses.NotificationHelper
import com.example.geofencelive.activities.GeofenceMapsActivity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task

interface LocationResultCallback {
    fun onLocationResult(latitude: Double, longitude: Double)
    fun onLocationError(errorMessage: String)
}

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val TAG = "GeofenceBroadcastReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        // TODO("GeofenceBroadcastReceiver.onReceive() is not implemented")

        //  Toast.makeText(context, "Geofence triggered",  Toast.LENGTH_LONG).show();
       // val cloudMessaging = CloudMessgingService()

        val sharedPreferences: SharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("userEmail", null)

        val notificationHelper : NotificationHelper = NotificationHelper(context)

        val geofencingEvent  = GeofencingEvent.fromIntent(intent);

        val firebaseHelper = FirestoreHelper();

        if (geofencingEvent != null) {
            if(geofencingEvent.hasError()){
                Log.d(TAG, "OnReceive : Error receiving geofence event...")
                return;
            }

            val geofenceList : MutableList<Geofence>? = geofencingEvent.triggeringGeofences;
            for (geofence in geofenceList!!) {
                Log.d(TAG, "onReceive: geofence list  $userEmail " + geofence.requestId)

                val geofenceId = geofence.requestId

                val transitionType: Number = geofencingEvent.geofenceTransition

                when (transitionType) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> {
                        getCurrentLocation(context, object : LocationResultCallback {
                            override fun onLocationResult(latitude: Double, longitude: Double) {
                                Log.d(TAG, "Current location: ($latitude, $longitude)")
                                val currentCoordinates = LatLng(latitude, longitude)
                                Toast.makeText(context, "GEOFENCE_TRANSITION_ENTER Current Coordinates : $currentCoordinates", Toast.LENGTH_LONG).show()
                                firebaseHelper.postTransitionEvents("Geofence entered", currentCoordinates, userEmail!!)
                                firebaseHelper.updateGeofenceEnteredFlag(geofenceId);


//                            notificationHelper.sendHighPriorityNotification(
//                                "GEOFENCE_TRANSITION_ENTER", "Current Coordinates : $currentCoordinates",
//                                GeofenceMapsActivity::class.java,currentCoordinates
//                            )
                                // Handle the location result here (e.g., update UI or send to server)


                            }

                            override fun onLocationError(errorMessage: String) {
                                Log.e(TAG, errorMessage)
                            }
                        })
                    }

//                Geofence.GEOFENCE_TRANSITION_DWELL -> {
//                    Toast.makeText(context, "GEOFENCE_TRANSITION_DWELL", Toast.LENGTH_LONG).show()
//                    notificationHelper.sendHighPriorityNotification(
//                        "GEOFENCE_TRANSITION_DWELL", "",
//                        MapsActivity::class.java
//                    )
//                }

                    Geofence.GEOFENCE_TRANSITION_EXIT -> {

                        getCurrentLocation(context, object : LocationResultCallback {
                            override fun onLocationResult(latitude: Double, longitude: Double) {
                                Log.d(TAG, "Current location: ($latitude, $longitude)")
                                val currentCoordinates = LatLng(latitude, longitude)
                                firebaseHelper.postTransitionEvents("Geofence exit", currentCoordinates, userEmail!!)
                                Toast.makeText(context, "GEOFENCE_TRANSITION_EXIT Current Coordinates : $currentCoordinates", Toast.LENGTH_LONG).show()
                                firebaseHelper.updateGeofenceEnteredFlag(geofenceId);
//                            notificationHelper.sendHighPriorityNotification(
//                                "GEOFENCE_TRANSITION_EXIT",
//                                "Current Coordinates : $currentCoordinates",
//                                GeofenceMapsActivity::class.java, currentCoordinates
//                            )
                                // Handle the location result here (e.g., update UI or send to server)
                            }

                            override fun onLocationError(errorMessage: String) {
                                Log.e(TAG, errorMessage)
                            }
                        })
                    }
                }

            }

            val transitionType: Number = geofencingEvent.geofenceTransition;

//            when (transitionType) {
//                Geofence.GEOFENCE_TRANSITION_ENTER -> {
//                    getCurrentLocation(context, object : LocationResultCallback {
//                        override fun onLocationResult(latitude: Double, longitude: Double) {
//                            Log.d(TAG, "Current location: ($latitude, $longitude)")
//                            val currentCoordinates = LatLng(latitude, longitude)
//                            Toast.makeText(context, "GEOFENCE_TRANSITION_ENTER Current Coordinates : $currentCoordinates", Toast.LENGTH_LONG).show()
//                            firebaseHelper.postTransitionEvents("Geofence entered", currentCoordinates, userEmail!!)
//
//
////                            notificationHelper.sendHighPriorityNotification(
////                                "GEOFENCE_TRANSITION_ENTER", "Current Coordinates : $currentCoordinates",
////                                GeofenceMapsActivity::class.java,currentCoordinates
////                            )
//                            // Handle the location result here (e.g., update UI or send to server)
//
//
//                        }
//
//                        override fun onLocationError(errorMessage: String) {
//                            Log.e(TAG, errorMessage)
//                        }
//                    })
//                }
//
////                Geofence.GEOFENCE_TRANSITION_DWELL -> {
////                    Toast.makeText(context, "GEOFENCE_TRANSITION_DWELL", Toast.LENGTH_LONG).show()
////                    notificationHelper.sendHighPriorityNotification(
////                        "GEOFENCE_TRANSITION_DWELL", "",
////                        MapsActivity::class.java
////                    )
////                }
//
//                Geofence.GEOFENCE_TRANSITION_EXIT -> {
//
//                    getCurrentLocation(context, object : LocationResultCallback {
//                        override fun onLocationResult(latitude: Double, longitude: Double) {
//                            Log.d(TAG, "Current location: ($latitude, $longitude)")
//                            val currentCoordinates = LatLng(latitude, longitude)
//                            firebaseHelper.postTransitionEvents("Geofence exit", currentCoordinates, userEmail!!)
//                            Toast.makeText(context, "GEOFENCE_TRANSITION_EXIT Current Coordinates : $currentCoordinates", Toast.LENGTH_LONG).show()
////                            notificationHelper.sendHighPriorityNotification(
////                                "GEOFENCE_TRANSITION_EXIT",
////                                "Current Coordinates : $currentCoordinates",
////                                GeofenceMapsActivity::class.java, currentCoordinates
////                            )
//                            // Handle the location result here (e.g., update UI or send to server)
//                        }
//
//                        override fun onLocationError(errorMessage: String) {
//                            Log.e(TAG, errorMessage)
//                        }
//                    })
//                }
//            }
        }else{
            Toast.makeText(context, "Geofence event null", Toast.LENGTH_SHORT).show()
        }
    }


    private fun getCurrentLocation(context: Context, callback: LocationResultCallback) {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        try {
            fusedLocationProviderClient.lastLocation.addOnCompleteListener { task: Task<Location?> ->
                if (task.isSuccessful && task.result != null) {
                    val location = task.result
                    if (location != null) {
                        callback.onLocationResult(location.latitude, location.longitude)
                    }
                } else {
                    callback.onLocationError("Failed to get location.")
                }
            }
        } catch (e: SecurityException) {
            callback.onLocationError("Location permission not granted: ${e.message}")
        }
    }
}