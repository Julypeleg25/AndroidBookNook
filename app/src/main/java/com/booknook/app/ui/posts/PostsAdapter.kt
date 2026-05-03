package com.booknook.app.ui.posts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.booknook.app.R
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.databinding.RowPostBinding
import com.booknook.app.util.formatRelativeTime
import com.booknook.app.util.loadRemoteImage
import java.text.NumberFormat

class PostsAdapter(
    private val currentUserId: String?,
    private val onClick: (String) -> Unit,
    private val showEngagement: Boolean = true,
    private val onLike: ((String) -> Unit)? = null,
    private val onEdit: ((String) -> Unit)? = null,
    private val onDelete: ((String) -> Unit)? = null
) : ListAdapter<PostEntity, PostsAdapter.Holder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(RowPostBinding.inflate(LayoutInflater.from(parent.context), parent, false), currentUserId, onClick, showEngagement, onLike, onEdit, onDelete)
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
        private val showEngagement: Boolean,
        private val onLike: ((String) -> Unit)?,
        private val onEdit: ((String) -> Unit)?,
        private val onDelete: ((String) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: PostEntity) {
            val context = binding.root.context
            binding.title.text = post.bookTitle
            binding.author.text = post.bookAuthor
            binding.rating.rating = post.rating.toFloat()
            binding.meta.text = context.getString(
                R.string.post_meta_format,
                post.username,
                context.formatRelativeTime(post.createdAt)
            )

            val infoParts = mutableListOf<String>()
            if (!post.bookGenre.isNullOrBlank()) infoParts.add(post.bookGenre)
            if (post.bookPageCount != null && post.bookPageCount > 0) {
                infoParts.add(context.resources.getQuantityString(R.plurals.book_pages, post.bookPageCount, post.bookPageCount))
            }
            
            if (infoParts.isNotEmpty()) {
                binding.bookInfo.text = infoParts.joinToString("  •  ")
                binding.bookInfo.visibility = android.view.View.VISIBLE
            } else {
                binding.bookInfo.visibility = android.view.View.GONE
            }
            
            val isOwnPost = post.userId == currentUserId
            if (showEngagement) {
                binding.engagementLayout.visibility = android.view.View.VISIBLE
                val numberFormat = NumberFormat.getIntegerInstance()
                binding.tvLikesCount.text = numberFormat.format(post.likesCount)
                binding.tvCommentsCount.text = numberFormat.format(post.commentsCount)

                val likeIcon = if (post.isLikedByUser) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                binding.btnLike.setIconResource(likeIcon)

                if (isOwnPost) {
                    binding.btnLike.isEnabled = false
                    binding.btnLike.alpha = 0.5f
                    binding.btnLike.setOnClickListener(null)
                } else {
                    binding.btnLike.isEnabled = true
                    binding.btnLike.alpha = 1.0f
                    binding.btnLike.setOnClickListener { onLike?.invoke(post.id) }
                }
            } else {
                binding.engagementLayout.visibility = android.view.View.GONE
            }
            
            binding.thumb.loadRemoteImage(post.bookThumbnail, R.drawable.book_placeholder)

            binding.root.setOnClickListener { onClick(post.id) }

            if (isOwnPost && onEdit != null && onDelete != null) {
                binding.actionLayout.visibility = android.view.View.VISIBLE
                binding.btnEdit.setOnClickListener { onEdit(post.id) }
                binding.btnDelete.setOnClickListener { onDelete(post.id) }
            } else {
                binding.actionLayout.visibility = android.view.View.GONE
            }
        }
    }
}
