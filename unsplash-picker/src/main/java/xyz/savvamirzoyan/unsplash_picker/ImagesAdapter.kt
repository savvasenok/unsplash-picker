package xyz.savvamirzoyan.unsplash_picker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.imageview.ShapeableImageView
import xyz.savvamirzoyan.unsplash_picker.model.UnsplashPhotoUi

internal class ImagesAdapter(
    private val isForCarousel: Boolean = false,
    private val onLoadMoreCallback: () -> Unit,
    private val onImageClick: (photoId: String) -> Unit
) : ListAdapter<UnsplashPhotoUi, ImageViewHolder>(GroupCareRequestsAdapterDiffUtil) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val viewGroup: View = if (isForCarousel)
            LayoutInflater.from(parent.context).inflate(R.layout.layout_selected_image, parent, false) as View
        else
            LayoutInflater.from(parent.context).inflate(R.layout.layout_search_result_image, parent, false) as View
        return if (isForCarousel) ImageViewHolder.Deletable(viewGroup) { position ->
            try {
                onImageClick(getItem(position).id)
            } catch (_: IndexOutOfBoundsException) {
            }
        }
        else ImageViewHolder.Checkable(viewGroup) { position ->
            try {
                onImageClick(getItem(position).id)
            } catch (_: IndexOutOfBoundsException) {
            }
        }
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {

        if (position == itemCount - 1) onLoadMoreCallback()

        val item = getItem(position)

        holder.bind(item)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)
        payloads.filterIsInstance<Boolean>().forEach { holder.bind(it) }
    }

    private object GroupCareRequestsAdapterDiffUtil : DiffUtil.ItemCallback<UnsplashPhotoUi>() {
        override fun areItemsTheSame(oldItem: UnsplashPhotoUi, newItem: UnsplashPhotoUi) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: UnsplashPhotoUi, newItem: UnsplashPhotoUi) =
            oldItem == newItem

        override fun getChangePayload(oldItem: UnsplashPhotoUi, newItem: UnsplashPhotoUi): Any? {
            return if (oldItem.isChecked != newItem.isChecked) newItem.isChecked else null
        }
    }
}

internal sealed class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    abstract fun bind(item: UnsplashPhotoUi)
    open fun bind(isChecked: Boolean) = Unit

    internal class Checkable(
        view: View,
        private val onItemClick: (position: Int) -> Unit
    ) : ImageViewHolder(view) {

        private val imageView: ImageView by lazy { view.findViewById(R.id.iv_picture) }
        private val checkbox: MaterialCheckBox by lazy { view.findViewById(R.id.cb_selected) }

        init {
            imageView.setOnClickListener { onItemClick(adapterPosition) }
        }

        override fun bind(item: UnsplashPhotoUi) {
            @Suppress("DEPRECATION")
            Glide.with(imageView)
                .load(item.thumb)
                .thumbnail(0.1f)
                .placeholder(null)
                .into(imageView)

            bind(item.isChecked)
        }

        override fun bind(isChecked: Boolean) {
            checkbox.isChecked = isChecked
            checkbox.isVisible = isChecked
        }
    }

    internal class Deletable(
        view: View,
        private val onItemClick: (position: Int) -> Unit
    ) : ImageViewHolder(view) {

        private val imageView: ShapeableImageView by lazy { view.findViewById(R.id.iv_picture) }

        init {
            view.findViewById<View>(R.id.button_cancel).setOnClickListener { onItemClick(adapterPosition) }
        }

        override fun bind(item: UnsplashPhotoUi) {
            Glide.with(imageView)
                .load(item.thumb)
                .placeholder(null)
                .into(imageView)
        }
    }
}