package xyz.savvamirzoyan.unsplash_picker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.savvamirzoyan.unsplash_picker.model.UnsplashPhoto
import xyz.savvamirzoyan.unsplash_picker.model.UnsplashPhotoUi

class UnsplashPhotoGalleryActivity : AppCompatActivity(R.layout.activity_gallery) {

    private var lastSelectedAmount = 0

    private var lastLoadedPage = 1
    private var lastLoadedSearchPage = 1

    private val appbar by lazy { findViewById<AppBarLayout>(R.id.appbar) }
    private val rvImages by lazy { findViewById<RecyclerView>(R.id.rv_images) }
    private val containerSelectMultiple by lazy { findViewById<MaterialCardView>(R.id.container_selectMultiple) }
    private val buttonCancel by lazy { findViewById<MaterialButton>(R.id.button_cancel) }
    private val buttonContinue by lazy { findViewById<MaterialButton>(R.id.button_continue) }
    private val tvCounter by lazy { findViewById<MaterialTextView>(R.id.tv_counter) }
    private val rvSelectedPhotos by lazy { findViewById<RecyclerView>(R.id.rv_selectedPhotos) }
    private val etSearchQuery by lazy { findViewById<TextInputEditText>(R.id.et_search_query) }
    private val tvNoPhotos by lazy { findViewById<MaterialTextView>(R.id.tv_noPhotos) }

    private val isSingleSelectionMode: Boolean by lazy { intent.extras?.getBoolean(KEY_IS_SINGLE_SELECT)!! }
    private val smallSize by lazy { resources.getDimensionPixelSize(R.dimen.small_size) }
    private val bigSize by lazy { resources.getDimensionPixelSize(R.dimen.big_size) }
    private val selectedImageHeight by lazy {
        resources.getDimensionPixelSize(R.dimen.selected_image_recycler_view_height).toFloat()
    }

    private val animationManager by lazy { AnimationManager() }

    private val loadedImagesFlow = MutableStateFlow<List<UnsplashPhoto>>(emptyList())
    private val selectedImagesFlow = MutableStateFlow<List<String>>(emptyList())
    private val loadedImagesUiFlow = combine(
        loadedImagesFlow.onEach { images -> images.onEach { Glide.with(this).load(it).preload(500, 500) } },
        selectedImagesFlow
    ) { images, selectedIds ->
        images.map { UnsplashPhotoUi(id = it.id, thumb = it.thumb, isChecked = it.id in selectedIds) }
    }
        .flowOn(Dispatchers.Default)
        .onEach { imagesUi ->

            tvNoPhotos.isVisible = imagesUi.isEmpty()

            val onlySelected = imagesUi.filter { it.isChecked }

            if (onlySelected.isEmpty() && lastSelectedAmount > 0) {

                animationManager.animateHeight(
                    from = selectedImageHeight,
                    to = 0f,
                    onStart = { selectedAdapter.submitList(onlySelected) },
                    onChange = { rvSelectedPhotos.updateLayoutParams { height = it } }
                )
            } else if (onlySelected.size == 1 && lastSelectedAmount == 0) {

                animationManager.animateHeight(
                    from = 0f,
                    to = selectedImageHeight,
                    onEnd = { selectedAdapter.submitList(onlySelected) },
                    onChange = { rvSelectedPhotos.updateLayoutParams { height = it } }
                )
            } else {
                selectedAdapter.submitList(onlySelected)
            }

            adapter.submitList(imagesUi)
            lastSelectedAmount = onlySelected.size
        }.stateIn(lifecycleScope, SharingStarted.Eagerly, emptyList())

    private val adapter: ImagesAdapter by lazy {
        if (isSingleSelectionMode) ImagesAdapter(false, ::loadMore, ::returnSingleResult)
        else ImagesAdapter(false, ::loadMore) { id ->

            val selectedPhotos = selectedImagesFlow.value.toMutableList()

            if (selectedPhotos.contains(id)) selectedPhotos.remove(id)
            else selectedPhotos.add(id)

            selectedImagesFlow.value = selectedPhotos

            updateMultipleSelectionContainerContent()
        }
    }

    private val selectedAdapter: ImagesAdapter by lazy {
        ImagesAdapter(true, ::loadMore) { photoId ->
            val selectedPhotos = selectedImagesFlow.value.toMutableList()
            selectedPhotos.remove(photoId)
            selectedImagesFlow.value = selectedPhotos

            updateMultipleSelectionContainerContent()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupView(savedInstanceState?.getStringArray(KEY_SAVE_INSTANCE_STATE)?.toList() ?: emptyList())
        loadMore()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArray(KEY_SAVE_INSTANCE_STATE, selectedImagesFlow.value.toTypedArray())
    }

    private fun setupView(selectedPhotosBefore: List<String>) {

        containerSelectMultiple.isVisible = !isSingleSelectionMode

        selectedImagesFlow.value = selectedPhotosBefore

        setupTopPaddingForContent()
        setupClickListeners()
        setupInsets()
        setupAdapters()
        setupSearchQueryListener()
        updateMultipleSelectionContainerContent()
    }

    private fun setupSearchQueryListener() {

        etSearchQuery.addTextChangedListener {
            val text = it.toString()

            if (text.isNotBlank()) {
                lifecycleScope.launch {
                    val photos = loadPhotos(lastLoadedSearchPage, text)

                    lastLoadedSearchPage++

                    loadedImagesFlow.emit(loadedImagesFlow.value + photos)
                }
            }
        }
    }

    private fun setupTopPaddingForContent() {
        appbar.doOnPreDraw {
            rvImages.setPadding(rvImages.paddingLeft, it.height, rvImages.paddingRight, rvImages.paddingBottom)
            rvImages.scrollToPosition(0)
        }
    }

    private fun setupAdapters() {
        rvImages.adapter = adapter

        if (!isSingleSelectionMode) rvSelectedPhotos.adapter = selectedAdapter
    }

    private fun setupClickListeners() {
        buttonCancel.setOnClickListener {
            selectedImagesFlow.value = emptyList()
            updateMultipleSelectionContainerContent()
        }

        buttonContinue.setOnClickListener {

            val loaded = loadedImagesFlow.value
            val selectedIds = selectedImagesFlow.value

            val selected = ArrayList(loaded.filter { it.id in selectedIds })

            val intent = Intent().apply { putParcelableArrayListExtra(KEY_PHOTOS, selected) }
            setResult(Activity.RESULT_OK, intent)

            finish()
        }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(appbar) { _, windowInsets ->

            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            appbar.updatePadding(top = insets.top, left = insets.left, right = insets.right)

            rvImages.updatePadding(
                left = insets.left + smallSize,
                right = insets.right + smallSize,
                bottom = insets.bottom + containerSelectMultiple.height + containerSelectMultiple.marginBottom * 2
            )

            val lpSelectMultiple = (containerSelectMultiple.layoutParams as ViewGroup.MarginLayoutParams)
            lpSelectMultiple.setMargins(
                insets.left + bigSize,
                insets.top + bigSize,
                insets.right + bigSize,
                insets.bottom + bigSize,
            )
            containerSelectMultiple.layoutParams = lpSelectMultiple

            WindowInsetsCompat.CONSUMED
        }
    }

    private fun loadMore() {

        lifecycleScope.launch {
            val photos = loadPhotos(lastLoadedPage)

            lastLoadedPage++
            loadedImagesFlow.emit(loadedImagesFlow.value + photos)
        }
    }

    private fun updateMultipleSelectionContainerContent() {
        val count = selectedImagesFlow.value.size

        buttonCancel.isEnabled = count != 0
        tvCounter.isEnabled = count != 0
        buttonContinue.isEnabled = count != 0

        tvCounter.text = resources.getQuantityString(R.plurals.pictures, count, count)
    }

    private fun returnSingleResult(photoId: String) {

        val loaded = loadedImagesFlow.value
        val photo = loaded.find { it.id == photoId }

        val intent = Intent().apply { putParcelableArrayListExtra(KEY_PHOTOS, arrayListOf(photo)) }
        setResult(Activity.RESULT_OK, intent)

        finish()
    }

    companion object {

        private const val KEY_SAVE_INSTANCE_STATE = "KEY_SAVE_INSTANCE_STATE"
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