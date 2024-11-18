package com.usaclean.frenchconnectionuser.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.usaclean.frenchconnectionuser.databinding.ItemDesignConversationLayoutBinding
import com.usaclean.frenchconnectionuser.model.Conversation
import com.usaclean.frenchconnectionuser.utils.UserSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversationAdapter(val context: Context, val list: ArrayList<Conversation>) :
    RecyclerView.Adapter<ConversationAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemDesignConversationLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        var model = list?.get(position)
        holder.binding.apply {
            if (model?.fromID.equals(UserSession.user.id)) {
                senderLayout.root.visibility = View.VISIBLE
                senderLayout.senderContentTV.text = model!!.content
                reciverLayout.root.visibility = View.GONE
                senderLayout.timeTV.text = convertFormat(model.timestamp)
            } else {
                reciverLayout.root.visibility = View.VISIBLE
                reciverLayout.reciverContantTV.text = model!!.content
                senderLayout.root.visibility = View.GONE
                reciverLayout.timeTV.text = convertFormat(model.timestamp)
            }
        }

    }

    inner class ViewHolder(val binding: ItemDesignConversationLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    private fun convertFormat(inputDate: Long): String {
        val date = Date(inputDate)
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(date)
    }

}