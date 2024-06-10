package xyz.savvamirzoyan.unsplash_picker

import android.app.Activity
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.card.MaterialCardView

internal class ViewInsetsManager {

    fun applyProperInsets(
        activity: Activity,
        insetLeft: Int,
        insetTop: Int,
        insetRight: Int,
        insetBottom: Int,
        appBarLayout: AppBarLayout,
        multipleSelectorContainer: MaterialCardView?,
        recyclerview: RecyclerView,
        searchRecyclerview: RecyclerView,
        isWideEnough: Boolean,
    ) {

        val smallSize = activity.resources.getDimension(R.dimen.small_size).toInt()
        val bigSize = activity.resources.getDimension(R.dimen.big_size).toInt()

        applyInsetsForAppBar(appBarLayout, insetLeft, insetRight, isWideEnough)
        applyInsetsForRecyclerView(recyclerview, insetLeft, insetRight, smallSize, isWideEnough)
        applyInsetsForRecyclerView(searchRecyclerview, insetLeft, insetRight, smallSize, isWideEnough)

        multipleSelectorContainer?.let {
            applyInsetsForMultipleSelectorContainer(
                multipleSelectorContainer,
                insetTop,
                insetBottom,
                insetRight,
                insetLeft,
                bigSize
            )
        }
    }

    private fun applyInsetsForRecyclerView(
        recyclerview: RecyclerView,
        insetLeft: Int,
        insetRight: Int,
        margin: Int,
        isWideEnough: Boolean,
    ) {

        val multiplier = if (isWideEnough) 0 else 1

        recyclerview.updatePadding(left = insetLeft + margin, right = insetRight * multiplier + margin)
    }

    private fun applyInsetsForMultipleSelectorContainer(
        multipleSelectorContainer: MaterialCardView,
        insetTop: Int,
        insetBottom: Int,
        insetRight: Int,
        insetLeft: Int,
        margin: Int,
    ) {
        multipleSelectorContainer.updateLayoutParams<MarginLayoutParams> {
            bottomMargin = insetBottom + margin
            topMargin = insetTop + margin
            rightMargin = insetRight + margin
            leftMargin = insetLeft + margin
        }
    }

    private fun applyInsetsForAppBar(
        appbar: AppBarLayout,
        insetLeft: Int,
        insetRight: Int,
        isWideEnough: Boolean,
    ) {

        val multiplier = if (isWideEnough) 0 else 1

        appbar.updatePadding(left = insetLeft, right = insetRight * multiplier)
    }
}
