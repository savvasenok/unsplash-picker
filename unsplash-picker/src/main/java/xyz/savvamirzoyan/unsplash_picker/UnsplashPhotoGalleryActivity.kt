package xyz.savvamirzoyan.unsplash_picker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.EdgeEffect
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

// TODO: make 3-button navigation  contrast. Is not visible on light theme on light surface
// TODO: API 34, multiple select, search, rotate left: fix padding of inner search when something is searched

class UnsplashPhotoGalleryActivity : AppCompatActivity(R.layout.activity_gallery) {

    private val viewModel by lazy {
        ViewModelProvider(
            owner = this,
            factory = UnsplashViewModelFactory(isSingleSelectionMode)
        )[UnsplashViewModel::class.java]
    }

    private val appbar by lazy { findViewById<AppBarLayout>(R.id.appbar) }
    private val rvImages by lazy { findViewById<RecyclerView>(R.id.rv_images) }
    private val rvSearchResults by lazy { findViewById<RecyclerView>(R.id.rv_searchResults) }
    private val containerSelectMultiple by lazy { findViewById<MaterialCardView>(R.id.container_selectMultiple) }
    private val buttonCancel by lazy { findViewById<MaterialButton>(R.id.button_cancel) }
    private val buttonContinue by lazy { findViewById<MaterialButton>(R.id.button_continue) }
    private val tvCounter by lazy { findViewById<MaterialTextView>(R.id.tv_counter) }
    private val rvSelectedPhotos by lazy { findViewById<RecyclerView>(R.id.rv_selectedPhotos) }

    private val searchView by lazy { findViewById<SearchView>(R.id.search_view) }
    private val searchBar by lazy { findViewById<SearchBar>(R.id.search_bar) }

    private val isSingleSelectionMode: Boolean by lazy { intent.extras?.getBoolean(KEY_IS_SINGLE_SELECT)!! }
    private val selectedImageHeight by lazy {
        resources.getDimensionPixelSize(R.dimen.selected_image_recycler_view_height).toFloat()
    }

    private val animationManager by lazy { AnimationManager() }
    private val viewInsetsManager by lazy { ViewInsetsManager() }

    private val isWideEnough: Boolean by lazy {

        val dm = resources.displayMetrics

        val triggerWidthDp = 840f
        val triggerWidthPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, triggerWidthDp, dm)

        val totalWidth = dm.widthPixels

        totalWidth > triggerWidthPx
    }

    private val adapter: ImagesAdapter by lazy {
        ImagesAdapter(false, viewModel::loadMoreImages, viewModel::onImageClick)
    }

    private val selectedAdapter: ImagesAdapter by lazy {
        ImagesAdapter(true, viewModel::loadMoreImages, viewModel::onImageClick)
    }

    private val searchAdapter: ImagesAdapter by lazy {
        ImagesAdapter(false, viewModel::loadMoreSearch, viewModel::onImageClick)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DynamicColors.applyToActivitiesIfAvailable(application)


        setupView()
        setupFlowListeners()
    }

    private fun setupFlowListeners() {

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.isImageContainerVisibleFlow.collect { containerSelectMultiple.isVisible = it }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.imagesListUiFlow.collect { adapter.submitList(it) }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.imagesSearchedListUiFlow.collect { searchAdapter.submitList(it) }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {

                var lastAmount = 0

                viewModel.imagesSelectedListUiFlow.collect { selected ->

                    if (selected.isEmpty() && lastAmount > 0) {

                        animationManager.animateHeight(
                            from = selectedImageHeight,
                            to = 0f,
                            onStart = { selectedAdapter.submitList(selected) },
                            onChange = { rvSelectedPhotos.updateLayoutParams { height = it } }
                        )
                    } else if (selected.size == 1 && lastAmount == 0) {

                        animationManager.animateHeight(
                            from = 0f,
                            to = selectedImageHeight,
                            onEnd = {
                                selectedAdapter.submitList(selected) {
                                    rvSelectedPhotos.smoothScrollToPosition(selected.lastIndex)
                                }
                            },
                            onChange = { rvSelectedPhotos.updateLayoutParams { height = it } }
                        )
                    } else {

                        val isReduced = lastAmount > selected.size

                        selectedAdapter.submitList(selected) {
                            if (selected.isNotEmpty() && !isReduced) rvSelectedPhotos.smoothScrollToPosition(selected.lastIndex)
                        }
                    }

                    lastAmount = selected.size
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.counterFlow.collect { count ->

                    buttonCancel.isEnabled = count != 0
                    tvCounter.isEnabled = count != 0
                    buttonContinue.isEnabled = count != 0

                    tvCounter.text = resources.getQuantityString(R.plurals.pictures, count, count)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.resultFlow.collect { intent -> setResult(Activity.RESULT_OK, intent);finish() }
            }
        }
    }

    private fun setupView() {

        appbar.addOnOffsetChangedListener { _, verticalOffset ->
            val alpha = 1 + (verticalOffset / appbar.height.toFloat())
            appbar.alpha = alpha
        }

        setupTopPaddingForContent()
        setupClickListeners()
        setupInsets()
        setupRecyclerViews()
        setupSearchQueryListener()
    }

    private fun setupSearchQueryListener() {
        searchView.editText.addTextChangedListener { viewModel.search(it?.toString() ?: "") }
    }

    private fun setupTopPaddingForContent() {
        appbar.doOnPreDraw {
            rvImages.setPadding(rvImages.paddingLeft, it.height, rvImages.paddingRight, rvImages.paddingBottom)
            rvImages.scrollToPosition(0)
        }
    }

    private fun setupRecyclerViews() {
        rvImages.adapter = adapter

        if (!isSingleSelectionMode) {
            rvSelectedPhotos.adapter = selectedAdapter
            rvSelectedPhotos.edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
                override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
                    return EdgeEffect(view.context).apply { color = Color.parseColor("#FFFFFF") }
                }
            }
        }

        rvImages.edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
            override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
                return EdgeEffect(view.context).apply { color = Color.parseColor("#FFFFFF") }
            }
        }

        rvSearchResults.adapter = searchAdapter
    }

    private fun setupClickListeners() {
        buttonCancel.setOnClickListener { viewModel.clearSelected() }
        buttonContinue.setOnClickListener { viewModel.onFinished() }
    }

    private fun setupInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(findViewById<ViewGroup>(android.R.id.content).rootView) { _, windowInsets ->

            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            viewInsetsManager.applyProperInsets(
                activity = this,
                insetLeft = insets.left,
                insetTop = insets.top,
                insetRight = insets.right,
                insetBottom = insets.bottom,
                appBarLayout = appbar,
                multipleSelectorContainer = containerSelectMultiple,
                recyclerview = rvImages,
                searchRecyclerview = rvSearchResults,
                isWideEnough = isWideEnough
            )

            searchBar.updateLayoutParams<MarginLayoutParams> {
                topMargin = insets.top + resources.getDimensionPixelSize(R.dimen.big_size)
            }

            try {
                findViewById<View>(R.id.status_bar_background).updateLayoutParams<CoordinatorLayout.LayoutParams> {
                    height = insets.top
                }
            } catch (exception: ClassCastException) {
                findViewById<View>(R.id.status_bar_background).updateLayoutParams<RelativeLayout.LayoutParams> {
                    height = insets.top
                }
            }

            windowInsets
        }
    }

    companion object {

        internal const val KEY_IS_SINGLE_SELECT = "KEY_IS_SINGLE_SELECT"
        internal const val KEY_PHOTOS = "KEY_PHOTOS"

        fun newInstance(context: Context, isSingleSelect: Boolean): Intent =
            Intent(context, UnsplashPhotoGalleryActivity::class.java).apply {
                putExtra(
                    KEY_IS_SINGLE_SELECT, isSingleSelect
                )
            }
    }
}