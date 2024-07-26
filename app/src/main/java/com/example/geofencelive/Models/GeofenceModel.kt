package com.example.geofencelive.Models

import com.google.android.gms.maps.model.LatLng

data class GeofenceModel(
    val latLng: LatLng = LatLng(0.0,0.0),
    val geofenceId : String = "",
    val geofenceRadius : Double = 500.0,
    val userEmail : String = "",
    val entryDeadline: Long,
    val enteredFlag : Boolean
)

