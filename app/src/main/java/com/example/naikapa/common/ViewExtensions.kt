package com.example.naikapa.common

import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun Fragment.toast(message: String) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
}

fun View.applyStatusBarTopPadding() {
    androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        view.setPadding(
            view.paddingLeft,
            insets.top,
            view.paddingRight,
            view.paddingBottom
        )
        windowInsets
    }
}

fun View.applyNavigationBarBottomPadding() {
    androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        view.setPadding(
            view.paddingLeft,
            view.paddingTop,
            view.paddingRight,
            insets.bottom
        )
        windowInsets
    }
}
