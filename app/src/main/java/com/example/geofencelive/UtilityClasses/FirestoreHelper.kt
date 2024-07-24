package com.example.geofencelive.UtilityClasses

import com.example.geofencelive.Models.GeofenceTransitionModel
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FirestoreHelper{
    val db = FirebaseFirestore.getInstance()
    val TAG = "Firestore class"
    val postCollections = db.collection("geofence")
    val postUsers = db.collection("users")
    val userGroupCollection = db.collection("userGroups")

    fun getUserGroups(){
        GlobalScope.launch {

        }
    }
    fun postTransitionEvents(transition: String, latLng: LatLng, userEmail:String){

        GlobalScope.launch {
            val nTransition = GeofenceTransitionModel(
                transition,
                latLng,
                userEmail,
                System.currentTimeMillis()
            )

            postCollections.document().set(nTransition)
        }
    }


}