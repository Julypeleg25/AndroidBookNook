package com.colman.booknook.ui.wishlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.colman.booknook.data.local.entities.WishlistEntity
import com.colman.booknook.databinding.RowBookBinding
import com.squareup.picasso.Picasso

class WishlistAdapter(
    private val onRemove: (WishlistEntity) -> Unit
) : RecyclerView.Adapter<WishlistAdapter.Holder>() {

    private val items = mutableListOf<WishlistEntity>()

    fun submit(list: List<WishlistEntity>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(RowBookBinding.inflate(LayoutInflater.from(parent.context), parent, false), onRemove)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size

    class Holder(
        private val binding: RowBookBinding,
        private val onRemove: (WishlistEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WishlistEntity) {
            binding.title.text = item.title
            binding.author.text = item.author
            if (!item.thumbnail.isNullOrBlank()) {
                Picasso.get().load(item.thumbnail).fit().centerCrop().into(binding.thumb)
            }
            binding.root.setOnLongClickListener { onRemove(item); true }
        }
    }
}
