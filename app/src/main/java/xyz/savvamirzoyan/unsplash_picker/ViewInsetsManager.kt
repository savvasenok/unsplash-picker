package xyz.savvamirzoyan.unsplash_picker

import android.app.Activity
import android.view.Surface
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.card.MaterialCardView

class ViewInsetsManager(
    private val isSingleSelect: Boolean,
    private val screenRotationManager: ScreenRotationManager,
) {

    fun applyProperInsets(
        activity: Activity,
        insetLeft: Int,
        insetTop: Int,
        insetRight: Int,
        insetBottom: Int,
        appBarLayout: AppBarLayout,
        multipleSelectorContainer: MaterialCardView?,
        recyclerview: RecyclerView,
    ) {

        val smallSize = activity.resources.getDimension(R.dimen.small_size).toInt()
        val bigSize = activity.resources.getDimension(R.dimen.big_size).toInt()

        val rotation = screenRotationManager.getScreenOrientation(activity)
        applyInsetsForAppBar(rotation, appBarLayout, insetLeft, insetTop, insetRight)
        applyInsetsForRecyclerView(rotation, recyclerview, insetLeft, insetRight, smallSize)

        multipleSelectorContainer?.let {
            applyInsetsForMultipleSelectorContainer(
                multipleSelectorContainer,
                insetTop,
                insetBottom,
                insetRight,
                bigSize
            )
        }
    }

    private fun applyInsetsForRecyclerView(
        rotation: Int,
        recyclerview: RecyclerView,
        insetLeft: Int,
        insetRight: Int,
        margin: Int,
    ) {
        if (isSingleSelect) {
            recyclerview.updatePadding(left = insetLeft + margin, right = insetRight + margin)
        } else {
            when (rotation) {
                Surface.ROTATION_0 ->
                    recyclerview.updatePadding(left = insetLeft + margin, right = insetRight + margin)

                Surface.ROTATION_90 ->
                    recyclerview.updatePadding(left = margin, right = margin)

                Surface.ROTATION_180 ->
                    recyclerview.updatePadding(left = insetLeft + margin, right = insetRight + margin)

                Surface.ROTATION_270 ->
                    recyclerview.updatePadding(left = insetLeft + margin, right = margin)
            }
        }
    }

    private fun applyInsetsForMultipleSelectorContainer(
        multipleSelectorContainer: MaterialCardView,
        insetTop: Int,
        insetBottom: Int,
        insetRight: Int,
        margin: Int,
    ) {
        multipleSelectorContainer.updateLayoutParams<MarginLayoutParams> {
            bottomMargin = insetBottom + margin
            topMargin = insetTop + margin
            rightMargin = insetRight + margin
        }
    }

    private fun applyInsetsForAppBar(
        rotation: Int,
        appbar: AppBarLayout,
        insetLeft: Int,
        insetTop: Int,
        insetRight: Int,
    ) {
        if (isSingleSelect) {
            appbar.updatePadding(top = insetTop, left = insetLeft, right = insetRight)
        } else {
            when (rotation) {
                Surface.ROTATION_0 -> appbar.updatePadding(top = insetTop, left = insetLeft, right = insetRight)
                Surface.ROTATION_90 -> appbar.updatePadding(top = insetTop, left = insetLeft)
                Surface.ROTATION_180 -> appbar.updatePadding(top = insetTop, left = insetLeft, right = insetRight)
                Surface.ROTATION_270 -> appbar.updatePadding(top = insetTop, left = insetLeft, right = insetRight)
            }
        }
    }
}
