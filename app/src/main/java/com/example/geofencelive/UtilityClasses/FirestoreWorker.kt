package com.example.geofencelive.UtilityClasses

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.geofencelive.activities.GeofenceMapsActivity
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.maps.model.LatLng



class FirestoreWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private val TAG = "FirestoreWorker"
    private val firestore = FirebaseFirestore.getInstance()
    //  private val auth = FirebaseAuth.getInstance()


    override fun doWork(): Result {
        listenToFirestore()
        return Result.success()
    }

    private fun listenToFirestore() {
        val processedEvents = getProcessedEvents(applicationContext)
        firestore.collection("geofence")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            val documentId = dc.document.id
                            val useremail = dc.document.getString("useremail")
                            val transition = dc.document.getString("transitionType")

                            val latLngMap = dc.document.get("latLng") as? Map<String, Any>
                            val latitude = latLngMap?.get("latitude") as? Double
                            val longitude = latLngMap?.get("longitude") as? Double

                            val currCoordinates = LatLng(latitude!!, longitude!!)

                            if (!processedEvents.contains(documentId)) {
                                saveProcessedEvent(applicationContext, documentId)
                                val notificationHelper = NotificationHelper(applicationContext)
                                notificationHelper.sendHighPriorityNotification(
                                    "$useremail Geofence event",
                                    "Transition type: $transition: Current Coordinates: $currCoordinates",
                                    GeofenceMapsActivity::class.java,
                                    currCoordinates
                                )
                            }


                            Log.d("Happy", useremail.toString())
                        }

                        DocumentChange.Type.MODIFIED -> {
                            // Handle modified case
                        }
                        DocumentChange.Type.REMOVED -> {
                            // Handle removed case
                        }
                    }
                }
            }
    }


    private fun getProcessedEvents(context: Context): MutableSet<String> {
        val sharedPreferences = context.getSharedPreferences("ProcessedEvents", Context.MODE_PRIVATE)
        return sharedPreferences.getStringSet("events", mutableSetOf()) ?: mutableSetOf()
    }

    private fun saveProcessedEvent(context: Context, documentId: String) {
        val sharedPreferences = context.getSharedPreferences("ProcessedEvents", Context.MODE_PRIVATE)
        val processedEvents = getProcessedEvents(context)
        processedEvents.add(documentId)
        sharedPreferences.edit().putStringSet("events", processedEvents).apply()
    }

    private fun clearProcessedEvents(context: Context) {
        val sharedPreferences = context.getSharedPreferences("ProcessedEvents", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
    }


}

