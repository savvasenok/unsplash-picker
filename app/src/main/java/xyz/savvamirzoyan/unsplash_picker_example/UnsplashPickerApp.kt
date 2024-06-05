package xyz.savvamirzoyan.unsplash_picker_example

import android.app.Application
import com.google.android.material.color.DynamicColors
import xyz.savvamirzoyan.unsplash_picker.UnsplashPickerConfig

class UnsplashPickerApp : Application(){

    override fun onCreate() {
        super.onCreate()

        UnsplashPickerConfig.init("")
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}