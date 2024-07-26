package com.example.geofencelive.UtilityClasses

import android.util.Log
import com.example.geofencelive.Models.GeofenceModel
import com.example.geofencelive.Models.GeofenceTransitionModel
import com.example.geofencelive.Models.UserModal
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
import kotlin.math.log

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

    fun addGeofenceData(id:String, radius:Double, latLng: LatLng, email:String, entryDeadline:Long){
        GlobalScope.launch {
            val nGeofence = GeofenceModel(
                latLng,
                id,
                radius,
                email,
                entryDeadline,
                false

            )

            geofenceCollection.document().set(nGeofence);
        }
    }

    fun updateGeofenceEnteredFlag(id: String) {
        val geofenceCollection = db.collection("geofenceData")
        geofenceCollection.whereEqualTo("geofenceId", id).get()
            .addOnSuccessListener { documents ->
                if (documents != null && !documents.isEmpty) {
                    val documentId = documents.documents[0].id
                    geofenceCollection.document(documentId).update("enteredFlag", true)
                        .addOnSuccessListener {
                            Log.d(TAG, "Geofence enteredFlag successfully updated!")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error updating enteredFlag", e)
                        }
                } else {
                    Log.d(TAG, "No geofence found with the given ID")
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting geofence: ", exception)
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
                val entryDeadline = document.getLong("entryDeadline")?:return@mapNotNull null
                val entered = document.getBoolean("enteredFlag")?:return@mapNotNull null

                GeofenceModel(latLng, id, radius, email,entryDeadline!!, entered!!)

            }

            withContext(Dispatchers.Main) {
                callback(geofences)
            }
        }

    }

    fun getGeofencebyBool(id:String): Boolean {

        var enteredFlag = false;

        getGeofenceById(id){geofence->

            enteredFlag = geofence?.enteredFlag ?: false

            Log.d("Entered flag", enteredFlag.toString())

        }

        return enteredFlag
    }

    fun getGeofenceById(id: String, callback: (GeofenceModel?) -> Unit) {
        val geofenceCollection = db.collection("geofenceData")
        geofenceCollection.whereEqualTo("geofenceId", id).get()
            .addOnSuccessListener { documents ->
                if (documents != null) {
                    val document = documents.documents[0]
                    val data = document.data
                    val latLngMap = data?.get("latLng") as? Map<String, Any>

                    val latLng = latLngMap?.let {
                        LatLng(
                            it["latitude"] as? Double ?: 0.0,
                            it["longitude"] as? Double ?: 0.0
                        )
                    } ?: LatLng(0.0, 0.0)

                    val geofence = GeofenceModel(
                        latLng = latLng, // Convert map back to LatLng
                        geofenceId = data?.get("geofenceId") as? String ?: "",
                        geofenceRadius = data?.get("geofenceRadius") as? Double ?: 500.0,
                        userEmail = data?.get("userEmail") as? String ?: "",
                        entryDeadline = data?.get("entryDeadline") as? Long ?: 0L,
                        enteredFlag = data?.get("enteredFlag") as? Boolean ?: false
                    )

                    callback(geofence)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.w("FirestoreHelper", "Error getting geofence: ", exception)
                callback(null)
            }
    }


//    fun getGeofenceById(id : String, callback: (GeofenceModel?) -> Unit){
//        val geofenceCollection = db.collection("geofenceData")
//        geofenceCollection.whereEqualTo("geofenceId", id).get()
//            .addOnSuccessListener { documents ->
//                if (documents != null && !documents.isEmpty) {
//                    val geofence = documents.documents[0].toObject(GeofenceModel::class.java)
//                    callback(geofence)
//                } else {
//                    callback(null)
//                }
//            }
//            .addOnFailureListener { exception ->
//                Log.w("FirestoreHelper", "Error getting geofence: ", exception)
//                callback(null)
//            }
//    }




}