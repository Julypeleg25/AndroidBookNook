package com.colman.booknook.ui.books

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.colman.booknook.R
import com.booknook.app.databinding.FragmentBookSearchBinding
import com.booknook.app.model.Model
import kotlinx.coroutines.launch

class BookSearchFragment : Fragment(R.layout.fragment_book_search) {

    private lateinit var binding: FragmentBookSearchBinding
    private val adapter = BookAdapter { book ->
        val action = BookSearchFragmentDirections.actionBookSearchToCreatePost(
            bookId = book.id,
            bookTitle = book.title,
            bookAuthor = book.author,
            bookThumbnail = book.thumbnail
        )
        findNavController().navigate(action)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentBookSearchBinding.bind(view)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.doSearchBtn.setOnClickListener {
            val q = binding.queryInput.text.toString().trim()
            if (q.isEmpty()) return@setOnClickListener

            setLoading(true)
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val books = Model.searchBooks(q)
                    adapter.submit(books)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), e.message ?: "Search failed", Toast.LENGTH_SHORT).show()
                } finally {
                    setLoading(false)
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.doSearchBtn.isEnabled = !isLoading
    }
}
