package com.booknook.app.ui.posts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.booknook.app.data.local.entities.CommentEntity
import com.booknook.app.databinding.RowCommentBinding
import com.booknook.app.util.loadRemoteImage
import java.text.DateFormat
import java.util.Date

import com.booknook.app.R

class CommentsAdapter : ListAdapter<CommentEntity, CommentsAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = RowCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CommentViewHolder(private val binding: RowCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: CommentEntity) {
            binding.username.text = comment.username
            binding.text.text = comment.text
            binding.time.text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(comment.createdAt))

            binding.avatar.loadRemoteImage(comment.userAvatarUrl, R.drawable.ic_default_avatar)
        }
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<CommentEntity>() {
        override fun areItemsTheSame(oldItem: CommentEntity, newItem: CommentEntity): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CommentEntity, newItem: CommentEntity): Boolean = oldItem == newItem
    }
}
