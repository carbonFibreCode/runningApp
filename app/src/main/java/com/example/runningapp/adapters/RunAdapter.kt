package com.example.runningapp.adapters

import android.icu.util.Calendar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.runningapp.db.Run
import com.example.runningapp.R
import com.example.runningapp.databinding.ItemRunBinding
import com.example.runningapp.other.TrackingUtility
import java.text.SimpleDateFormat
import java.util.Locale

class RunAdapter : RecyclerView.Adapter<RunAdapter.RunViewHolder>() { //we will setup this runAdapter in RunFragment

    inner class RunViewHolder(val binding: ItemRunBinding) : RecyclerView.ViewHolder(binding.root)
    //we will implement a list differ, it is a tool that calculates the difference between thee two lists and also returns them,
    //it is very efficient for recyclerview, as it updates only those items which are different among the two given lists
    val diffCallback = object : DiffUtil.ItemCallback<Run>(){
        override fun areItemsTheSame(
            oldItem: Run,
            newItem: Run
        ): Boolean {
            return oldItem.id == newItem.id //here it only verifies the id of the item not th content, it could differ like the different speed and date etc.
        }

        override fun areContentsTheSame(
            oldItem: Run,
            newItem: Run
        ): Boolean {
            return oldItem.hashCode() == newItem.hashCode() //if hashcodes match means the items are 100% same
        }
    }

    val differ = AsyncListDiffer(this, diffCallback)

    fun submitList(list: List<Run>) = differ.submitList(list)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RunViewHolder {
        val binding = ItemRunBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RunViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: RunViewHolder,
        position: Int
    ) {
        val run = differ.currentList[position]
        holder.binding.apply {
            Glide.with(root.context).load(run.img).into(ivRunImage)

            val calendar = Calendar.getInstance().apply {
                timeInMillis = run.timeStamp //timestamp is the date in millis
            }
            //we will use the calendar object to format the time in millis in a format
            val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
            tvDate.text = dateFormat.format(calendar.time)

            val avgSpeed = "${run.avgSpeedInKMH}KM/H"
            tvAvgSpeed.text = avgSpeed

            val distanceInKm = "${run.distanceInMeters / 1000f}KM"
            tvDistance.text = distanceInKm

            tvTime.text = TrackingUtility.getFormattedStopWatchTime(run.timeInMillis)

            val caloriesBurned = "${run.caloriesBurned}kCal"
            tvCalories.text = caloriesBurned
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
}