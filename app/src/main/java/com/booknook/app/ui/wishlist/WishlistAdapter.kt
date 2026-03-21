package com.booknook.app.ui.wishlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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
            binding.title.text = item.title
            binding.author.text = item.author
            
            Picasso.get()
                .load(item.thumbnail)
                .placeholder(com.booknook.app.R.drawable.book_placeholder)
                .error(com.booknook.app.R.drawable.book_placeholder)
                .fit()
                .centerCrop()
                .into(binding.thumb)
                
            binding.root.setOnLongClickListener { onRemove(item); true }
        }
    }
}
