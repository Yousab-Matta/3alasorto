package com.example.alasorto.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.example.alasorto.dataClass.Attendance
import com.example.alasorto.AttendanceHistoryFragment
import com.example.alasorto.R
import java.text.SimpleDateFormat

class AttHistoryAdapter(
    private val attList: ArrayList<Attendance>,
    private val onClickListener: OnClickListener,
) : RecyclerView.Adapter<AttHistoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance, parent, false)
        return ViewHolder(view, onClickListener)
    }

    @SuppressLint("SimpleDateFormat")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val attendance = attList[position]
        val sdf = SimpleDateFormat("EEE, d, MMM, HH:mm:ss")
        val date = attendance.Date
        holder.attDate.text = sdf.format(date!!)
    }

    override fun getItemCount(): Int {
        return attList.size
    }

    class ViewHolder(
        itemView: View,
        listener: OnClickListener,
    ) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        val attDate: Button = itemView.findViewById(R.id.btn_att_name)
        private val mListener = listener

        init {
            attDate.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            mListener.onClick(layoutPosition)
        }
    }

    interface OnClickListener {
        fun onClick(position: Int)
    }
}