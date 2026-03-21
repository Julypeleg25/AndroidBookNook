package com.booknook.app.ui.wishlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.booknook.app.data.local.entities.ReadlistEntity
import com.booknook.app.databinding.RowBookBinding
import com.squareup.picasso.Picasso

class ReadlistAdapter(
    private val onRemove: (ReadlistEntity) -> Unit
) : RecyclerView.Adapter<ReadlistAdapter.Holder>() {

    private val items = mutableListOf<ReadlistEntity>()

    fun submit(list: List<ReadlistEntity>) {
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
        private val onRemove: (ReadlistEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ReadlistEntity) {
            binding.title.text = item.title
            binding.author.text = item.author
            if (!item.thumbnail.isNullOrBlank()) {
                Picasso.get().load(item.thumbnail).fit().centerCrop().into(binding.thumb)
            }
            binding.root.setOnLongClickListener { onRemove(item); true }
        }
    }
}
