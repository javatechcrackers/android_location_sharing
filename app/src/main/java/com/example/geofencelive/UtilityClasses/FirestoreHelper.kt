package com.example.geofencelive.UtilityClasses

import android.util.Log
import com.example.geofencelive.Models.GeofenceModel
import com.example.geofencelive.Models.GeofenceTransitionModel
import com.example.geofencelive.Models.userToken
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreHelper{
    val db = FirebaseFirestore.getInstance()
    val TAG = "Firestore class"

    val postCollections = db.collection("geofence")
    val geofenceCollection = db.collection("geofenceData")
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

    fun addGeofenceData(id:String, radius:Double, latLng: LatLng, email:String){
        GlobalScope.launch {
            val nGeofence = GeofenceModel(
                latLng,
                id,
                radius,
                email

            )

            geofenceCollection.document().set(nGeofence);
        }
    }
    fun getGeofenceList(email: String, callback: (List<GeofenceModel>) -> Unit) {
        getGeofences(email) { list ->
            callback(list)
        }
    }

    fun saveUserToken(userEmail: String, token:String){
        val nToken = userToken(
            token,
            userEmail
        )

        val tokenCollection = db.collection("userFCMToken");
        tokenCollection.document().set(nToken)
    }
//    fun fetchGeofences(email: String, callback: (List<GeofenceModel>) -> Unit) {
//
//        getGeofences(email) { geofences ->
//            geofencesList.addAll(geofences)
//            callback(geofencesList)
//        }
//    }

    fun getGeofences(email: String, callback: (List<GeofenceModel>) -> Unit){

        GlobalScope.launch {
            val querySnapshot: QuerySnapshot = geofenceCollection.whereEqualTo("userEmail", email).get().await()

            val geofences = querySnapshot.documents.mapNotNull { document ->

                val id = document.getString("geofenceId") ?: return@mapNotNull null
                val radius = document.getDouble("geofenceRadius") ?: return@mapNotNull null
                val email = document.getString("userEmail") ?: return@mapNotNull null
                val latLngMap = document.get("latLng") as? Map<String, Any> ?: return@mapNotNull null
                val latitude = latLngMap["latitude"] as? Double ?: return@mapNotNull null
                val longitude = latLngMap["longitude"] as? Double ?: return@mapNotNull null
                val latLng = LatLng(latitude, longitude)

                GeofenceModel(latLng, id, radius, email)

            }

            withContext(Dispatchers.Main) {
                callback(geofences)
            }
        }

    }




}