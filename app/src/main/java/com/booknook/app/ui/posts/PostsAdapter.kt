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
    private val currentUserId: String?,
    private val onClick: (String) -> Unit,
    private val onLike: ((String) -> Unit)? = null,
    private val onEdit: ((String) -> Unit)? = null,
    private val onDelete: ((String) -> Unit)? = null
) : ListAdapter<PostEntity, PostsAdapter.Holder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(RowPostBinding.inflate(LayoutInflater.from(parent.context), parent, false), currentUserId, onClick, onLike, onEdit, onDelete)
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
        private val currentUserId: String?,
        private val onClick: (String) -> Unit,
        private val onLike: ((String) -> Unit)?,
        private val onEdit: ((String) -> Unit)?,
        private val onDelete: ((String) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: PostEntity) {
            binding.title.text = post.bookTitle
            binding.author.text = post.bookAuthor
            binding.rating.rating = post.rating.toFloat()
            binding.meta.text = "${post.username}  •  ${formatTime(post.createdAt)}"
            
            binding.tvLikesCount.text = post.likesCount.toString()
            binding.tvCommentsCount.text = post.commentsCount.toString()
            
            val isOwnPost = post.userId == currentUserId
            val likeIcon = if (post.isLikedByUser) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            binding.btnLike.setImageResource(likeIcon)
            
            // Interaction logic: Disable like for own posts
            if (isOwnPost) {
                binding.btnLike.isEnabled = false
                binding.btnLike.alpha = 0.5f // Visual hint
                binding.btnLike.setOnClickListener(null)
            } else {
                binding.btnLike.isEnabled = true
                binding.btnLike.alpha = 1.0f
                binding.btnLike.setOnClickListener { onLike?.invoke(post.id) }
            }
            
            Picasso.get()
                .load(post.bookThumbnail)
                .placeholder(R.drawable.book_placeholder)
                .error(R.drawable.book_placeholder)
                .fit()
                .centerCrop()
                .into(binding.thumb)

            binding.root.setOnClickListener { onClick(post.id) }

            if (isOwnPost && onEdit != null && onDelete != null) {
                binding.actionLayout.visibility = android.view.View.VISIBLE
                binding.btnEdit.setOnClickListener { onEdit(post.id) }
                binding.btnDelete.setOnClickListener { onDelete(post.id) }
            } else {
                binding.actionLayout.visibility = android.view.View.GONE
            }
        }

        private fun formatTime(timestamp: Long): String {
            val diff = System.currentTimeMillis() - timestamp
            return when {
                diff < 60000 -> "Just now"
                diff < 3600000 -> "${diff / 60000}m ago"
                diff < 86400000 -> "${diff / 3600000}h ago"
                else -> "${diff / 86400000}d ago"
            }
        }
    }
}
