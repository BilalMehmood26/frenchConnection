package com.usaclean.frenchconnectionuser.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.usaclean.frenchconnectionuser.R
import com.usaclean.frenchconnectionuser.databinding.ItemCardBinding
import com.usaclean.frenchconnectionuser.databinding.ItemSaveCardsBinding
import com.usaclean.frenchconnectionuser.model.PaymentMethodsResponse

class CardListAdapter(
    val context: Context,
    val list: ArrayList<PaymentMethodsResponse.PaymentMethod>,
    private val onCLick: () -> Unit
) :
    RecyclerView.Adapter<CardListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemCardBinding.inflate(
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
        val item = list[position]
        holder.binding.apply {
            when (item.brand) {
                "visa" -> cardBrandIv.setImageResource(R.drawable.ic_visa)
                "mastercard" -> cardBrandIv.setImageResource(R.drawable.ic_master)
            }

            lastFourTv.text = "**** **** **** ${item.last4}"
            expiryTv.text = "${item.exp_month}/${item.exp_year}"

            root.setOnClickListener {
                onCLick.invoke()
            }
        }
    }

    inner class ViewHolder(val binding: ItemCardBinding) :
        RecyclerView.ViewHolder(binding.root)

}