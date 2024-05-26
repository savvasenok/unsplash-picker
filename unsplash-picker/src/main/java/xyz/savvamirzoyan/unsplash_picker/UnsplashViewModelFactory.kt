package xyz.savvamirzoyan.unsplash_picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class UnsplashViewModelFactory(
    private val isSingleSelectMode: Boolean,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return UnsplashViewModel(isSingleSelectMode) as T
    }
}