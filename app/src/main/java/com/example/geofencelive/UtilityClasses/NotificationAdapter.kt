package com.example.geofencelive.UtilityClasses

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.geofencelive.Models.GeofenceTransitionModel
import com.example.geofencelive.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter(private val geofenceEventList: List<GeofenceTransitionModel>) :
    RecyclerView.Adapter<NotificationAdapter.GeofenceEventViewHolder>() {

    class GeofenceEventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val latitudeTextView: TextView = itemView.findViewById(R.id.notificationText)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GeofenceEventViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return GeofenceEventViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: GeofenceEventViewHolder, position: Int) {
        val currentEvent = geofenceEventList[position]
        val time = currentEvent.createdAt
        val date = Date(time!!)
        val format = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        val formattedDate = format.format(date)
        holder.latitudeTextView.text = "${currentEvent.useremail} ${currentEvent.transitionType} at $formattedDate"

    }

    override fun getItemCount() = geofenceEventList.size
}
