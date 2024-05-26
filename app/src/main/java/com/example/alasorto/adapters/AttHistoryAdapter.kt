package com.example.alasorto.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.alasorto.dataClass.Attendance
import com.example.alasorto.R
import java.math.RoundingMode

class AttHistoryAdapter(
    private val attList: ArrayList<Attendance>,
    private val getAttendance: (Attendance) -> Unit,
    private val handleAttendance: (Attendance) -> Unit
) : RecyclerView.Adapter<AttHistoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_button, parent, false)
        return ViewHolder(view, this)
    }

    @SuppressLint("SimpleDateFormat")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val attendance = attList[position]
        "${attendance.day}/${attendance.month}/${attendance.year}"
            .also {
                holder.attDate.text = it
            }
        "${attendance.attendancePercentage.toBigDecimal().setScale(2, RoundingMode.UP)}%".also { holder.statusTV.text = it }
    }

    override fun getItemCount(): Int {
        return attList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setAttendanceList(attendanceList: ArrayList<Attendance>) {
        attList.clear()
        attList.addAll(attendanceList)
        notifyDataSetChanged()
    }

    class ViewHolder(
        itemView: View,
        adapter: AttHistoryAdapter
    ) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener, View.OnLongClickListener {

        private val mAdapter = adapter

        val attDate: TextView = itemView.findViewById(R.id.tv_user_item_name)
        val statusTV: TextView = itemView.findViewById(R.id.tv_status)

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(p0: View?) {
            mAdapter.getAttendance(mAdapter.attList[adapterPosition])
        }

        override fun onLongClick(v: View?): Boolean {
            mAdapter.handleAttendance(mAdapter.attList[adapterPosition])
            return false
        }
    }
}