package com.booknook.app.ui.posts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.booknook.app.R
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.databinding.RowPostBinding
import com.booknook.app.util.displayPostImageUrl
import com.booknook.app.util.formatRelativeTime
import com.booknook.app.util.loadRemoteImage
import com.booknook.app.util.toShortGenreList
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
        return Holder(
            RowPostBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            currentUserId,
            onClick,
            showEngagement,
            onLike,
            onEdit,
            onDelete
        )
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
            bindPostText(post)
            bindBookInfo(post)
            bindEngagement(post)
            bindActions(post)
            binding.thumb.loadRemoteImage(post.displayPostImageUrl, R.drawable.book_placeholder)
            binding.root.setOnClickListener { onClick(post.id) }
        }

        private fun bindPostText(post: PostEntity) {
            val context = binding.root.context
            binding.title.text = post.bookTitle
            binding.author.text = post.bookAuthor
            binding.rating.rating = post.rating.toFloat()
            binding.meta.text = context.getString(
                R.string.post_meta_format,
                post.username,
                context.formatRelativeTime(post.createdAt)
            )
        }

        private fun bindBookInfo(post: PostEntity) {
            val context = binding.root.context
            val infoParts = mutableListOf<String>()
            post.bookGenre.toShortGenreList()?.let { genre -> infoParts.add(genre) }
            if (post.bookPageCount != null && post.bookPageCount > 0) {
                infoParts.add(
                    context.resources.getQuantityString(
                        R.plurals.book_pages,
                        post.bookPageCount,
                        post.bookPageCount
                    )
                )
            }

            if (infoParts.isNotEmpty()) {
                binding.bookInfo.text = infoParts.joinToString("  |  ")
                binding.bookInfo.visibility = View.VISIBLE
            } else {
                binding.bookInfo.visibility = View.GONE
            }
        }

        private fun bindEngagement(post: PostEntity) {
            if (!showEngagement) {
                binding.engagementLayout.visibility = View.GONE
                return
            }

            binding.engagementLayout.visibility = View.VISIBLE
            val numberFormat = NumberFormat.getIntegerInstance()
            binding.tvLikesCount.text = numberFormat.format(post.likesCount)
            binding.tvCommentsCount.text = numberFormat.format(post.commentsCount)
            binding.btnLike.setIconResource(
                if (post.isLikedByUser) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )

            val canLike = post.userId != currentUserId
            binding.btnLike.isEnabled = canLike
            binding.btnLike.alpha = if (canLike) ENABLED_ALPHA else DISABLED_ALPHA
            binding.btnLike.setOnClickListener(if (canLike) View.OnClickListener { onLike?.invoke(post.id) } else null)
        }

        private fun bindActions(post: PostEntity) {
            val canManagePost = post.userId == currentUserId && onEdit != null && onDelete != null
            binding.actionLayout.visibility = if (canManagePost) View.VISIBLE else View.GONE
            if (canManagePost) {
                binding.btnEdit.setOnClickListener { onEdit?.invoke(post.id) }
                binding.btnDelete.setOnClickListener { onDelete?.invoke(post.id) }
            }
        }
    }

    companion object {
        private const val ENABLED_ALPHA = 1.0f
        private const val DISABLED_ALPHA = 0.5f
    }
}
