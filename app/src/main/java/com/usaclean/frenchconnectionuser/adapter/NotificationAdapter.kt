package com.usaclean.frenchconnectionuser.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.usaclean.frenchconnectionuser.databinding.ItemNotificationBinding
import com.usaclean.frenchconnectionuser.model.Notification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter(val context: Context, private val list: List<Notification>) :
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemNotificationBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.apply {
            descriptionTv.text = item.message
            dateTimeTv.text = formatDateTime(item.timestamp!!)
        }

    }

    fun formatDateTime(millis: Long): String {
        val formatter = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.ENGLISH)
        val date = Date(millis)
        return formatter.format(date)
    }
}