package com.usaclean.frenchconnectionuser.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.usaclean.frenchconnectionuser.R
import com.usaclean.frenchconnectionuser.databinding.ItemRideHistoryBinding
import com.usaclean.frenchconnectionuser.model.Booking
import com.usaclean.frenchconnectionuser.model.User
import com.usaclean.frenchconnectionuser.utils.UserSession
import kotlin.math.round

class RideHistoryAdapter(
    val context: Context,
    private val list: ArrayList<Booking>
) : RecyclerView.Adapter<RideHistoryAdapter.ViewHolder>() {

    private val db = Firebase.firestore

    inner class ViewHolder(val binding: ItemRideHistoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemRideHistoryBinding.inflate(
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

            db.collection("Users").document(item.driverId!!).get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result.toObject(User::class.java)
                    nameTv.text = user!!.userName
                    if (user.image!!.isNotEmpty()) {
                        Glide.with(context).load(user.image).into(profileIv)
                    } else {
                        profileIv.setImageResource(R.drawable.main_logo)
                    }
                } else {
                    Log.d("LOGGER", "is Fail")
                    Toast.makeText(
                        context,
                        task.exception!!.message.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            ratingTv.text = item.rating.toString()
            distanceTv.text = item.distance

           timeTv.text = item.time
            priceTv.text = "$ ${item.price}"
            statusTv.text = item.status
            vehcileTv.text = "---"
            ratingBar.rating = item.rating!!.toFloat()
        }
    }
}