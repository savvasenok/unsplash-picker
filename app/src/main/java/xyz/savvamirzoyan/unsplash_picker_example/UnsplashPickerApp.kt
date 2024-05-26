package xyz.savvamirzoyan.unsplash_picker_example

import android.app.Application
import com.google.android.material.color.DynamicColors

class UnsplashPickerApp : Application(){

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}