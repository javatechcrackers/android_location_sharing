package com.example.geofencelive.Models

import com.google.android.gms.maps.model.LatLng

data class GeofenceTransitionModel(

    val transitionType: String = "",
    val latLng: LatLng = LatLng(0.0,0.0),
    val useremail: String ="",
    val createdAt: Long = 0L,

    )