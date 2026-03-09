package com.booknook.app.ui.posts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.booknook.app.R
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.databinding.RowPostBinding
import com.squareup.picasso.Picasso

class PostsAdapter(
    private val onClick: (String) -> Unit,
    private val onEdit: ((String) -> Unit)? = null,
    private val onDelete: ((String) -> Unit)? = null
) : ListAdapter<PostEntity, PostsAdapter.Holder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(RowPostBinding.inflate(LayoutInflater.from(parent.context), parent, false), onClick, onEdit, onDelete)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    object DiffCallback : DiffUtil.ItemCallback<PostEntity>() {
        override fun areItemsTheSame(oldItem: PostEntity, newItem: PostEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PostEntity, newItem: PostEntity): Boolean {
            return oldItem == newItem
        }
    }

    class Holder(
        private val binding: RowPostBinding,
        private val onClick: (String) -> Unit,
        private val onEdit: ((String) -> Unit)?,
        private val onDelete: ((String) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: PostEntity) {
            binding.title.text = post.bookTitle
            binding.author.text = post.bookAuthor
            binding.rating.rating = post.rating.toFloat()
            binding.meta.text = "⭐ ${post.rating}  •  ${post.commentsCount} comments  •  ${post.likesCount} likes"
            
            // Task 2: Add book pictures from Google Books API everywhere relevant
            // Use Picasso correctly with placeholder
            Picasso.get()
                .load(post.bookThumbnail)
                .placeholder(R.drawable.book_placeholder)
                .error(R.drawable.book_placeholder)
                .fit()
                .centerCrop()
                .into(binding.thumb)

            binding.root.setOnClickListener { onClick(post.id) }

            if (onEdit != null && onDelete != null) {
                binding.actionLayout.visibility = android.view.View.VISIBLE
                binding.btnEdit.setOnClickListener { onEdit(post.id) }
                binding.btnDelete.setOnClickListener { onDelete(post.id) }
            } else {
                binding.actionLayout.visibility = android.view.View.GONE
            }
        }
    }
}
