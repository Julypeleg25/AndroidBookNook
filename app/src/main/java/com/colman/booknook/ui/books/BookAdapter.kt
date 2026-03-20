package com.colman.booknook.ui.books

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.colman.booknook.databinding.RowBookBinding
import com.colman.booknook.domain.Book
import com.squareup.picasso.Picasso

class BookAdapter(
    private val onClick: (Book) -> Unit
) : RecyclerView.Adapter<BookAdapter.Holder>() {

    private val items = mutableListOf<Book>()

    fun submit(list: List<Book>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(RowBookBinding.inflate(LayoutInflater.from(parent.context), parent, false), onClick)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size

    class Holder(
        private val binding: RowBookBinding,
        private val onClick: (Book) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(book: Book) {
            binding.title.text = book.title
            binding.author.text = book.author
            if (!book.thumbnail.isNullOrBlank()) {
                Picasso.get().load(book.thumbnail).fit().centerCrop().into(binding.thumb)
            }
            binding.root.setOnClickListener { onClick(book) }
        }
    }
}
