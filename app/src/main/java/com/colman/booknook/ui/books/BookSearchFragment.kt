package com.colman.booknook.ui.books

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.colman.booknook.R
import com.colman.booknook.databinding.FragmentBookSearchBinding
import com.colman.booknook.model.Model
import kotlinx.coroutines.launch

class BookSearchFragment : Fragment(R.layout.fragment_book_search) {

    private lateinit var binding: FragmentBookSearchBinding
    private val adapter = BookAdapter { book ->
        findNavController().navigate(
            R.id.createPostFragment,
            bundleOf(
                "bookId" to book.id,
                "bookTitle" to book.title,
                "bookAuthor" to book.author,
                "bookThumbnail" to book.thumbnail
            )
        )
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
