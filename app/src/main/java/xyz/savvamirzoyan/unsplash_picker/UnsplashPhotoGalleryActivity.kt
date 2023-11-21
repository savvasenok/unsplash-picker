package xyz.savvamirzoyan.unsplash_picker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class UnsplashPhotoGalleryActivity : AppCompatActivity(R.layout.activity_gallery) {

    private var cachedPhotos: List<UnsplashPhoto> = emptyList()
    private var cachedPhotosUi: List<UnsplashPhotoUi> = emptyList()
    private val selectedPhotos = mutableSetOf<String>()
    private var lastLoadedPage = 1

    private val appbar by lazy { findViewById<AppBarLayout>(R.id.appbar) }
    private val searchView by lazy { findViewById<SearchView>(R.id.search_view) }
    private val rvImages by lazy { findViewById<RecyclerView>(R.id.rv_images) }
    private val rvSearchResultImages by lazy { findViewById<RecyclerView>(R.id.rv_searchResultImages) }
    private val containerSelectMultiple by lazy { findViewById<LinearLayout>(R.id.container_selectMultiple) }
    private val buttonCancel by lazy { findViewById<MaterialButton>(R.id.button_cancel) }
    private val buttonContinue by lazy { findViewById<MaterialButton>(R.id.button_continue) }
    private val tvCounter by lazy { findViewById<MaterialTextView>(R.id.tv_counter) }
    private val rvSelectedPhotos by lazy { findViewById<RecyclerView>(R.id.rv_selectedPhotos) }

    private val isSingleSelectionMode: Boolean by lazy { intent.extras?.getBoolean(KEY_IS_SINGLE_SELECT)!! }

    private val adapter: ImagesAdapter by lazy {
        if (isSingleSelectionMode) ImagesAdapter(false, ::loadMore, ::returnSingleResult)
        else ImagesAdapter(false, ::loadMore) { id ->
            if (selectedPhotos.contains(id)) selectedPhotos.remove(id)
            else selectedPhotos.add(id)

            updateCachedPhotosUi()
            updatedMultipleSelection()
            updateMultipleSelectionContainerContent()
        }
    }

    private val searchAdapter by lazy { ImagesAdapter(false, ::loadMore, ::returnSingleResult) }

    private val selectedAdapter: ImagesAdapter by lazy {
        ImagesAdapter(true, ::loadMore) { photoId ->
            selectedPhotos.remove(photoId)

            updateCachedPhotosUi()
            updatedMultipleSelection()
            updateMultipleSelectionContainerContent()
        }
    }

    private fun updatedMultipleSelection() {
        val list = selectedPhotos.map { selectedId -> cachedPhotosUi.find { it.id == selectedId } }

        if (list.isEmpty()) {
            (rvSelectedPhotos.parent as View).visibility = View.INVISIBLE
            lifecycleScope.launch { delay(300); selectedAdapter.submitList(list) }
        } else {
            (rvSelectedPhotos.parent as View).visibility = View.VISIBLE
            selectedAdapter.submitList(list)
        }

    }

    private fun updateCachedPhotosUi() {
        val updated = cachedPhotosUi.map { it.copy(isChecked = selectedPhotos.contains(it.id)) }.toList()
        adapter.submitList(updated)
        cachedPhotosUi = updated
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupView()
        loadMore()
    }

    private fun setupView() {
        appbar.doOnPreDraw {
            appbar
            rvImages.setPadding(rvImages.paddingLeft, it.height, rvImages.paddingRight, rvImages.paddingBottom)
            rvImages.scrollToPosition(0)
        }

        containerSelectMultiple.isVisible = !isSingleSelectionMode

        setupClickListeners()
        setupInsets()
        setupAdapters()
    }

    private fun setupAdapters() {
        rvImages.adapter = adapter
        rvSearchResultImages.adapter = searchAdapter

        if (!isSingleSelectionMode) rvSelectedPhotos.adapter = selectedAdapter
    }

    private fun setupClickListeners() {
        buttonCancel.setOnClickListener {
            selectedPhotos.clear()
            updateCachedPhotosUi()
            updateMultipleSelectionContainerContent()
        }

        buttonContinue.setOnClickListener {
            val selected = ArrayList(cachedPhotos.filter { selectedPhotos.contains(it.id) })

            setResult(Activity.RESULT_OK, Intent().apply {
                putParcelableArrayListExtra(KEY_PHOTOS, selected)
            })

            finish()
        }

        findViewById<SearchBar>(R.id.search_bar).apply {
            setNavigationOnClickListener { finish() }
        }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(appbar) { _, windowInsets ->

            val smallSize = resources.getDimensionPixelSize(R.dimen.small_size)
            val bigSize = resources.getDimensionPixelSize(R.dimen.big_size)

            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            appbar
                .updatePadding(top = insets.top, left = insets.left, right = insets.right)
            rvImages
                .updatePadding(
                    left = insets.left + smallSize,
                    right = insets.right + smallSize,
                    bottom = insets.bottom + containerSelectMultiple.height + containerSelectMultiple.marginBottom * 2
                )
            searchView
                .updatePadding(top = insets.top, left = insets.left, right = insets.right)

            val lp = (containerSelectMultiple.layoutParams as ViewGroup.MarginLayoutParams)
            lp.setMargins(
                insets.left + bigSize,
                0,
                insets.right + bigSize,
                insets.bottom + bigSize,
            )
            containerSelectMultiple.layoutParams = lp

            WindowInsetsCompat.CONSUMED
        }
    }

    private fun loadMore() {

        lifecycleScope.launch {
            val photos = loadPhotos(lastLoadedPage)
            val photosUi = photos.map { UnsplashPhotoUi(id = it.id, thumb = it.thumb, isChecked = false) }

            lastLoadedPage++
            cachedPhotos = cachedPhotos + photos
            cachedPhotosUi = cachedPhotosUi + photosUi
            adapter.submitList(cachedPhotosUi)

            findViewById<MaterialTextView>(R.id.tv_noPhotos).isVisible = cachedPhotosUi.isEmpty()
        }
    }

    private fun updateMultipleSelectionContainerContent() {
        val count = selectedPhotos.size

        buttonCancel.isVisible = count != 0
        buttonContinue.isEnabled = count != 0
        tvCounter.isVisible = count != 0

        if (count >= 1) {
            tvCounter.text = resources.getQuantityString(R.plurals.pictures, count, count)
        }
    }

    private fun returnSingleResult(photoId: String) {

        val photo = cachedPhotos.find { it.id == photoId }!!

        setResult(Activity.RESULT_OK, Intent().apply {
            putParcelableArrayListExtra(KEY_PHOTOS, arrayListOf(photo))
        })

        finish()
    }

    companion object {

        private const val KEY_IS_SINGLE_SELECT = "KEY_IS_SINGLE_SELECT"
        internal const val KEY_PHOTOS = "KEY_PHOTOS"

        fun newInstance(context: Context, isSingleSelect: Boolean): Intent =
            Intent(context, UnsplashPhotoGalleryActivity::class.java).apply {
                putExtra(
                    KEY_IS_SINGLE_SELECT,
                    isSingleSelect
                )
            }
    }
}