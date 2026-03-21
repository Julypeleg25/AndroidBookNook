package com.booknook.app.ui.posts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.databinding.RowPostBinding
import com.squareup.picasso.Picasso

class PostsAdapter(
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<PostsAdapter.Holder>() {

    private val items = mutableListOf<PostEntity>()

    fun submit(list: List<PostEntity>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(RowPostBinding.inflate(LayoutInflater.from(parent.context), parent, false), onClick)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class Holder(
        private val binding: RowPostBinding,
        private val onClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: PostEntity) {
            binding.title.text = post.bookTitle
            binding.author.text = post.bookAuthor
            binding.rating.rating = post.rating.toFloat()
            binding.meta.text = "⭐ ${post.rating}  •  ${post.commentsCount} comments  •  ${post.likesCount} likes"
            if (!post.bookThumbnail.isNullOrBlank()) {
                Picasso.get().load(post.bookThumbnail).fit().centerCrop().into(binding.thumb)
            }
            binding.root.setOnClickListener { onClick(post.id) }
        }
    }
}
