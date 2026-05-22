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

fun View.applyStatusBarTopPaddingTo(targetView: View, additionalPadding: Int = 0) {
    val initialPaddingTop = targetView.paddingTop
    androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
        val insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        targetView.setPadding(
            targetView.paddingLeft,
            initialPaddingTop + insets.top + additionalPadding,
            targetView.paddingRight,
            targetView.paddingBottom
        )
        windowInsets
    }
}

fun View.applyStatusBarTopMarginTo(targetView: View, additionalMargin: Int = 0) {
    val layoutParams = targetView.layoutParams as? android.view.ViewGroup.MarginLayoutParams
    val initialMarginTop = layoutParams?.topMargin ?: 0
    androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
        val insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        val params = targetView.layoutParams as? android.view.ViewGroup.MarginLayoutParams
        if (params != null) {
            params.topMargin = initialMarginTop + insets.top + additionalMargin
            targetView.layoutParams = params
        }
        windowInsets
    }
}
