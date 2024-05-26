package xyz.savvamirzoyan.unsplash_picker

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.savvamirzoyan.unsplash_picker.model.UnsplashPhoto
import xyz.savvamirzoyan.unsplash_picker.model.UnsplashPhotoUi

class UnsplashViewModel(
    private val isSingleSelectMode: Boolean,
) : ViewModel() {

    private val lastLoadedPageFlow = MutableStateFlow(1)
    private val lastLoadedSearchPageFlow = MutableStateFlow(1)
    private val selectedImagesFlow = MutableStateFlow(emptyList<UnsplashPhoto>())
    private val selectedImagesIdsFlow = selectedImagesFlow.map { list -> list.map { it.id } }
    private val searchQueryFlow = MutableStateFlow("")
    private val _resultFlow = MutableSharedFlow<Intent>(replay = 0)

    private val imagesListFlow: StateFlow<List<UnsplashPhoto>> = lastLoadedPageFlow
        .map { page -> loadPhotos(page) }
        .scan(emptyList<UnsplashPhoto>()) { accumulator, value -> accumulator + value }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val imagesSearchedListFlow: StateFlow<List<UnsplashPhoto>> = combine(
        lastLoadedSearchPageFlow,
        searchQueryFlow.onEach { lastLoadedSearchPageFlow.value = 1 }.filter { it.isNotBlank() }
    ) { page: Int, query: String -> page to query }
        .debounce(300)
        .map { pair -> /*is new query*/(pair.first == 1) to /*actual data*/(loadPhotos(pair.first, pair.second)) }
        .scan(emptyList<UnsplashPhoto>()) { acc, value -> if (value.first) value.second else acc + value.second }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val resultFlow: Flow<Intent> = _resultFlow.asSharedFlow()

    val imagesListUiFlow: Flow<List<UnsplashPhotoUi>> = imagesListFlow
        .map { list -> list.map { UnsplashPhotoUi(id = it.id, thumb = it.thumb, isChecked = false) } }
        .combine(selectedImagesIdsFlow) { images, selectedIds -> images.map { combine(it, selectedIds) } }

    val imagesSearchedListUiFlow: Flow<List<UnsplashPhotoUi>> = imagesSearchedListFlow
        .map { list -> list.map { UnsplashPhotoUi(id = it.id, thumb = it.thumb, isChecked = false) } }
        .combine(selectedImagesIdsFlow) { images, selectedIds -> images.map { combine(it, selectedIds) } }

    val imagesSelectedListUiFlow: Flow<List<UnsplashPhotoUi>> = selectedImagesFlow
        .map { list -> list.map { UnsplashPhotoUi(id = it.id, thumb = it.thumb, isChecked = true) } }

    val isImageContainerVisibleFlow: StateFlow<Boolean> = MutableStateFlow(!isSingleSelectMode).asStateFlow()

    val counterFlow: Flow<Int> = selectedImagesFlow.map { it.size }

    fun loadMoreImages() = lastLoadedPageFlow.update { it + 1 }


    fun loadMoreSearch() = lastLoadedSearchPageFlow.update { it + 1 }

    fun search(query: String) {
        searchQueryFlow.value = query.trim()
    }

    fun onImageClick(imageId: String) {

        if (isSingleSelectMode) {
            onFinished(imageId)
            return
        }

        val isChecked = selectedImagesFlow.value.find { source -> source.id == imageId } != null

        if (isChecked) {
            selectedImagesFlow.update { it.filter { source -> source.id != imageId } }
        } else {

            val image = imagesListFlow.value.find { source -> source.id == imageId }
                ?: imagesSearchedListFlow.value.find { source -> source.id == imageId }
                ?: return

            selectedImagesFlow.update { it + image }
        }
    }

    fun clearSelected() {
        selectedImagesFlow.value = emptyList()
    }

    fun onFinished() {
        val images: ArrayList<UnsplashPhoto> = ArrayList(selectedImagesFlow.value)

        val intent = Intent()
            .apply { putParcelableArrayListExtra(UnsplashPhotoGalleryActivity.KEY_PHOTOS, images) }
        viewModelScope.launch { _resultFlow.emit(intent) }
    }

    private fun onFinished(imageId: String) {

        val imageAsList: ArrayList<UnsplashPhoto> = (imagesListFlow.value.find { source -> source.id == imageId }
            ?: imagesSearchedListFlow.value.find { source -> source.id == imageId })
            ?.let { arrayListOf(it) }
            ?: return

        val intent = Intent()
            .apply { putParcelableArrayListExtra(UnsplashPhotoGalleryActivity.KEY_PHOTOS, imageAsList) }
        viewModelScope.launch { _resultFlow.emit(intent) }
    }

    private fun combine(image: UnsplashPhotoUi, selectedIds: List<String>): UnsplashPhotoUi =
        if (image.id in selectedIds) image.copy(isChecked = true) else image
}