package com.booknook.app.ui.wishlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.booknook.app.R
import com.booknook.app.data.local.entities.WishlistEntity
import com.booknook.app.databinding.RowBookBinding
import com.squareup.picasso.Picasso

class WishlistAdapter(
    private val onRemove: (WishlistEntity) -> Unit
) : ListAdapter<WishlistEntity, WishlistAdapter.Holder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(RowBookBinding.inflate(LayoutInflater.from(parent.context), parent, false), onRemove)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    object DiffCallback : DiffUtil.ItemCallback<WishlistEntity>() {
        override fun areItemsTheSame(oldItem: WishlistEntity, newItem: WishlistEntity): Boolean {
            return oldItem.key == newItem.key
        }

        override fun areContentsTheSame(oldItem: WishlistEntity, newItem: WishlistEntity): Boolean {
            return oldItem == newItem
        }
    }

    class Holder(
        private val binding: RowBookBinding,
        private val onRemove: (WishlistEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WishlistEntity) {
            val context = binding.root.context
            binding.title.text = item.title
            binding.author.text = item.author
            
            if (!item.genre.isNullOrBlank()) {
                binding.genre.text = item.genre
                binding.genre.visibility = android.view.View.VISIBLE
            } else {
                binding.genre.visibility = android.view.View.GONE
            }

            val metaParts = buildList {
                if (!item.publishedDate.isNullOrBlank()) {
                    add(context.getString(R.string.book_meta_published_format, item.publishedDate))
                }
                if (item.pageCount != null && item.pageCount > 0) {
                    add(context.resources.getQuantityString(R.plurals.book_pages, item.pageCount, item.pageCount))
                }
            }
            if (metaParts.isNotEmpty()) {
                binding.pageCount.text = metaParts.joinToString("\n")
                binding.pageCount.visibility = android.view.View.VISIBLE
            } else {
                binding.pageCount.visibility = android.view.View.GONE
            }

            Picasso.get()
                .load(item.thumbnail)
                .placeholder(R.drawable.book_placeholder)
                .error(R.drawable.book_placeholder)
                .fit()
                .centerCrop()
                .into(binding.thumb)

            binding.btnRemove.visibility = android.view.View.VISIBLE
            binding.btnRemove.contentDescription = context.getString(R.string.book_remove_button)
            binding.btnRemove.setOnClickListener { onRemove(item) }
        }
    }
}
