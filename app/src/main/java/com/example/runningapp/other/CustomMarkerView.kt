package com.example.runningapp.other

import android.content.Context
import android.icu.util.Calendar
import android.view.View
import android.widget.TextView
import com.example.runningapp.R
import com.example.runningapp.databinding.MarkerViewBinding
import com.example.runningapp.db.Run
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.SimpleDateFormat
import java.util.Locale

class CustomMarkerView(val runs : List<Run>, c : Context, layoutId : Int) : MarkerView(c, layoutId) {

    private val tvDate: TextView by lazy { findViewById(R.id.tvDate) }
    private val tvAvgSpeed: TextView by lazy { findViewById(R.id.tvAvgSpeed) }
    private val tvDistance: TextView by lazy { findViewById(R.id.tvDistance) }
    private val tvDuration: TextView by lazy { findViewById(R.id.tvDuration) }
    private val tvCaloriesBurned: TextView by lazy { findViewById(R.id.tvCaloriesBurned) }

    override fun getOffset(): MPPointF? {
        return MPPointF(-(width / 2f), -height.toFloat())
    }

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        super.refreshContent(e, highlight)
        if(e == null){
            return
        }
        val curRunId = e.x.toInt() //got the index from the mapEntry
        val run = runs[curRunId] //get the run at that particular index

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

        tvDuration.text = TrackingUtility.getFormattedStopWatchTime(run.timeInMillis)

        val caloriesBurned = "${run.caloriesBurned}kCal"
        tvCaloriesBurned.text = caloriesBurned
    }
}