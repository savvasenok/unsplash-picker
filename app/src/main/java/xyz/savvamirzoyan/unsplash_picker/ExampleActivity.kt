package xyz.savvamirzoyan.unsplash_picker

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView

class ExampleActivity : AppCompatActivity(R.layout.activity_example) {

    private val contract = registerForActivityResult(PickingImageFlowFromUnsplashContract()) { unsplashPhotos ->

        findViewById<LinearLayout>(R.id.container_images).apply {
            removeAllViews()
            unsplashPhotos.forEach {
                val view = ImageView(this@ExampleActivity)
                Glide.with(this).load(it.full).into(view)
                this.addView(view)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findViewById<MaterialButton>(R.id.button_select_single).setOnClickListener {
            startPicker(true)
        }

        findViewById<MaterialButton>(R.id.button_select_multiple).setOnClickListener {
            startPicker(false)
        }
    }

    private fun startPicker(isSingleSelection: Boolean) {
        contract.launch(isSingleSelection)
    }
}