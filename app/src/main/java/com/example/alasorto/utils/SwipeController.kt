package com.example.alasorto.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import kotlin.math.abs

class SwipeController(
    private val mContext: Context,
    private val swipeControllerActions: ISwipeControllerActions
) :
    ItemTouchHelper.Callback() {

    private val mReplyIcon: Drawable? = null
    private val mReplyIconBackground: Drawable? = null

    private var mCurrentViewHolder: ViewHolder? = null
    private var mView: View? = null

    private var mDx = 0f

    private val mReplyButtonProgress = 0f
    private val mLastReplyButtonAnimationTime: Long = 0

    private var mSwipeBack = false
    private val mIsVibrating = false
    private var mStartTracking = false

    private val mBackgroundColor = 0x20606060

    private val mReplyBackgroundOffset = 18

    private val mReplyIconXOffset = 12
    private val mReplyIconYOffset = 11

    private var mIsSwipeEnabled = true

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: ViewHolder
    ): Int {
        mView = viewHolder.itemView
        return if (viewHolder.itemView.layoutDirection == LAYOUT_DIRECTION_LTR) {
            makeMovementFlags(
                ItemTouchHelper.ACTION_STATE_IDLE,
                ItemTouchHelper.RIGHT
            )
        } else {
            makeMovementFlags(
                ItemTouchHelper.ACTION_STATE_IDLE,
                ItemTouchHelper.LEFT
            )
        }
    }

    fun setSwipeEnabled(isSwipeEnabled: Boolean) {
        mIsSwipeEnabled = isSwipeEnabled
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return mIsSwipeEnabled
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: ViewHolder,
        target: ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
    }

    override fun convertToAbsoluteDirection(flags: Int, layoutDirection: Int): Int {
        if (mSwipeBack) {
            mSwipeBack = false
            return 0
        }
        return super.convertToAbsoluteDirection(flags, layoutDirection)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            setTouchListener(recyclerView, viewHolder)
        }
        if (mView!!.translationX < convertToDp(200) || dX < mDx)
            super.onChildDraw(
                c,
                recyclerView,
                viewHolder,
                dX / 2,
                dY,
                actionState,
                isCurrentlyActive
            )
        mDx = dX
        mStartTracking = true
        mCurrentViewHolder = viewHolder
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setTouchListener(recyclerView: RecyclerView, viewHolder: ViewHolder) {
        recyclerView.setOnTouchListener { _, p1 ->
            if (p1 != null) {
                mSwipeBack =
                    p1.action == MotionEvent.ACTION_CANCEL || p1.action == MotionEvent.ACTION_UP
            }
            if (mSwipeBack) {
                if (abs(mView!!.translationX) >= convertToDp(20)) {
                    swipeControllerActions.onSwipePerformed(viewHolder.adapterPosition)
                }
            }
            false
        }
    }

    private fun convertToDp(pixels: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            pixels.toFloat(),
            mContext.resources.displayMetrics
        ).toInt()
    }
}

interface ISwipeControllerActions {
    fun onSwipePerformed(position: Int)
}