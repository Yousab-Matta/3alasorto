package com.example.alasorto

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.core.content.ContextCompat

class TextWatcherCreateUser(
    private val view: EditText,
    private val context: Context
) : TextWatcher {

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        view.setCompoundDrawablesRelativeWithIntrinsicBounds(
            0,
            0,
            0,
            0
        )
        view.background =
            ContextCompat.getDrawable(context, R.drawable.custom_input)
    }

    override fun afterTextChanged(p0: Editable?) {
    }
}