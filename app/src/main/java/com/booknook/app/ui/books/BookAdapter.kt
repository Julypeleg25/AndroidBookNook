package com.booknook.app.ui.books

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.booknook.app.R
import com.booknook.app.databinding.RowBookBinding
import com.booknook.app.domain.Book
import com.squareup.picasso.Picasso

class BookAdapter(
    private val onClick: (Book) -> Unit
) : ListAdapter<Book, BookAdapter.Holder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(RowBookBinding.inflate(LayoutInflater.from(parent.context), parent, false), onClick)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    object DiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem == newItem
        }
    }

    class Holder(
        private val binding: RowBookBinding,
        private val onClick: (Book) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(book: Book) {
            binding.title.text = book.title
            binding.author.text = book.author
            Picasso.get()
                .load(book.thumbnail)
                .placeholder(R.drawable.book_placeholder)
                .error(R.drawable.book_placeholder)
                .fit()
                .centerCrop()
                .into(binding.thumb)
            binding.root.setOnClickListener { onClick(book) }
        }
    }
}
