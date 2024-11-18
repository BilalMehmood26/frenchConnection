package com.usaclean.frenchconnectionuser.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.usaclean.frenchconnectionuser.databinding.ItemScheduleRideBinding
import com.usaclean.frenchconnectionuser.model.Booking
import com.usaclean.frenchconnectionuser.utils.UserSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduleAdapter(
    val context: Context,
    private val list: ArrayList<Booking>
) : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {


    inner class ViewHolder(val binding: ItemScheduleRideBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemScheduleRideBinding.inflate(
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
            nameTv.text = UserSession.user.userName
            timeDateTv.text = formatDateTime(item.bookingDate!!)
            ratingTv.text = item.rating.toString()
            pickUpTv.text = item.pickUp!!.address
            dropoffTv.text = item.destinations[0].address
        }
    }


    fun formatDateTime(millis: Long): String {
        val formatter = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.ENGLISH)
        val date = Date(millis)
        return formatter.format(date)
    }
}