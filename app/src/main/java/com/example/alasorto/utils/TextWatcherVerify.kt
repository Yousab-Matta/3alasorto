package com.example.alasorto.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.example.alasorto.AuthActivity

class TextWatcherVerify(
    private val prevET: EditText?,
    private val nextET: EditText?,
    private val mActivity: AuthActivity
) :
    TextWatcher {
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        if (p0?.length == 1 && nextET != null) {
            nextET.requestFocus()
        } else if (p0?.length == 0 && prevET != null) {
            prevET.requestFocus()
        } else if (p0?.length == 1 && nextET == null || p0?.length == 0 && prevET == null) {
            mActivity.hideKeyboard()
        }
    }

    override fun afterTextChanged(p0: Editable?) {
    }
}