package com.usaclean.frenchconnectionuser.adapter

import android.annotation.SuppressLint
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.usaclean.frenchconnectionuser.R
import com.usaclean.frenchconnectionuser.databinding.CalendarCellBinding
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@RequiresApi(Build.VERSION_CODES.O)
class CalendarAdapter(
    private val daysOfMonth: ArrayList<String>,
    private val selectedDate: LocalDate,
    private val onItemListener: (String,Long) -> Unit
) :
    RecyclerView.Adapter<CalendarAdapter.ViewHolder>() {

    interface OnItemListener {
        fun onDateClicked(message: String, dayText: Long)
    }

    private var selectedPosition = -1

    init {
        val dayOfMonth = selectedDate.dayOfMonth.toString()
        selectedPosition = daysOfMonth.indexOf(dayOfMonth)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            CalendarCellBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        holder.binding.cellDayText.text = daysOfMonth[position]

        // Highlight the selected date
        if (position == selectedPosition) {
            holder.binding.cellDayText.setBackgroundResource(R.drawable.tab_background)
            holder.binding.cellDayText.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.white
                )
            )

        } else {
            holder.binding.cellDayText.setBackgroundResource(0)
            holder.binding.cellDayText.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.white
                )
            )
        }
        holder.itemView.setOnClickListener {
            if (daysOfMonth[position] != null && daysOfMonth[position] != "") {
                val previousPosition = selectedPosition
                selectedPosition = position

                val selectedDateString =
                    "${daysOfMonth[position]}/${selectedDate.monthValue}/${selectedDate.year}"

                try {
                    val formatter = DateTimeFormatter.ofPattern("d/M/yyyy")
                    val selectedLocalDate = LocalDate.parse(selectedDateString, formatter)
                    val timestamp =
                        selectedLocalDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond() * 1000


                    onItemListener.invoke(selectedDateString, timestamp)

                    notifyItemChanged(previousPosition)
                    notifyItemChanged(selectedPosition)
                } catch (e: DateTimeParseException) {
                    e.printStackTrace()
                    onItemListener.invoke(e.message.toString(), 0L)
                }

                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)

            }
        }

    }

    override fun getItemCount(): Int {
        return daysOfMonth.size
    }

    inner class ViewHolder(val binding: CalendarCellBinding) : RecyclerView.ViewHolder(binding.root)
}