package xyz.savvamirzoyan.unsplash_picker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import xyz.savvamirzoyan.unsplash_picker.model.UnsplashPhoto

class PickingImageFlowFromUnsplashContract : ActivityResultContract<Boolean, List<UnsplashPhoto>>() {

    override fun createIntent(context: Context, input: Boolean): Intent =
        UnsplashPhotoGalleryActivity.newInstance(context, input)

    override fun parseResult(resultCode: Int, intent: Intent?): List<UnsplashPhoto> {
        if (resultCode != Activity.RESULT_OK) return emptyList()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) intent
            ?.getParcelableArrayListExtra(UnsplashPhotoGalleryActivity.KEY_PHOTOS, UnsplashPhoto::class.java)
            ?.toList()
            ?: emptyList()
        else (intent
            ?.getParcelableArrayListExtra<UnsplashPhoto>(UnsplashPhotoGalleryActivity.KEY_PHOTOS) as? ArrayList<UnsplashPhoto>)
            ?.toList()
            ?: emptyList()
    }
}