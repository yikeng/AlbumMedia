package com.demons.media.utils.media

import android.view.View
import androidx.databinding.BindingAdapter

object MediaAdapter {
    @BindingAdapter(value = ["isShow"])
    @JvmStatic
    fun showOperate(view: View, isSingle: Boolean) {
        if (isSingle) {
            view.visibility = View.GONE
        } else {
            view.visibility = View.VISIBLE
        }
    }
}