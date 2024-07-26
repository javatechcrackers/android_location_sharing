


package com.example.geofencelive.UtilityClasses

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.geofencelive.activities.GeofenceMapsActivity
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class FirestoreWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private val TAG = "FirestoreWorker"
    private val firestore = FirebaseFirestore.getInstance()
    //  private val auth = FirebaseAuth.getInstance()


    override fun doWork(): Result {
        listenToFirestore(applicationContext)
        return Result.success()
    }

    private fun listenToFirestore(context: Context) {

        val sharedPreferences = context.getSharedPreferences("MyPrefs", Activity.MODE_PRIVATE)
     //   val userEmail = sharedPreferences.getString("userEmail", null)

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
                            val time = dc.document.getLong("createdAt")

                            val date = Date(time!!)
                            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            val formattedDate = format.format(date)

                            val latLngMap = dc.document.get("latLng") as? Map<String, Any>
                            val latitude = latLngMap?.get("latitude") as? Double
                            val longitude = latLngMap?.get("longitude") as? Double

                            val currCoordinates = LatLng(latitude!!, longitude!!)

                            val docId = sharedPreferences.getString(documentId.toString(), null)

                            val currentTimeMillis = System.currentTimeMillis()
                            val fiveMinutesInMillis = 5 * 60 * 1000 // 5 minutes in milliseconds



                            val fiveMinutesAgoMillis = currentTimeMillis - fiveMinutesInMillis


                            val isWithinLastFiveMinutes = time >= fiveMinutesAgoMillis
                            val notificationHelper = NotificationHelper(applicationContext)

                            if(docId == null && isWithinLastFiveMinutes && transition != "No Event Happened"){
                                val editor = sharedPreferences.edit()
                                editor.putString(documentId.toString(), documentId.toString())
                               // Save device identifier if needed
                                editor.apply()

                                notificationHelper.sendHighPriorityNotification(
                                    "$useremail Geofence event",
                                    "Transition type: $transition: Current Coordinates: $currCoordinates at $formattedDate" ,
                                    GeofenceMapsActivity::class.java,
                                    currCoordinates
                                )

                                Log.d("Notification sent ", useremail.toString())
                            }

                            if(docId == null && isWithinLastFiveMinutes && transition == "No Event Happened"){
                                val editor = sharedPreferences.edit()
                                editor.putString(documentId.toString(), documentId.toString())
                                // Save device identifier if needed
                                editor.apply()
                                notificationHelper.sendHighPriorityNotification(
                                    "No Event Happened",
                                    "$useremail didn't entered in geofence within given time. Time limit exhausted at $formattedDate" ,
                                    GeofenceMapsActivity::class.java,
                                    currCoordinates
                                )
                            }





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

