package xyz.savvamirzoyan.unsplash_picker

import android.app.Activity


class ScreenRotationManager {
    fun getScreenOrientation(activity: Activity) = activity.windowManager.defaultDisplay.rotation
}