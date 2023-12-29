package xyz.savvamirzoyan.unsplash_picker

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager

class KeyboardManager {

    fun hideKeyboard(activity: Activity) {
        val view = activity.currentFocus
        val methodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        methodManager.hideSoftInputFromWindow(view?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    fun showKeyboard(activity: Activity) {
        val view = activity.currentFocus
        val methodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        methodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
}