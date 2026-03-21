package com.booknook.app.ui.books

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.booknook.app.R
import com.booknook.app.databinding.FragmentBookSearchBinding
import com.google.android.material.snackbar.Snackbar

class BookSearchFragment : Fragment(R.layout.fragment_book_search) {

    private var _binding: FragmentBookSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookSearchViewModel by viewModels()

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
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBookSearchBinding.bind(view)

        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        observeViewModel()

        binding.doSearchBtn.setOnClickListener {
            val q = binding.queryInput.text.toString().trim()
            if (q.isNotEmpty()) {
                viewModel.searchBooks(q)
            } else {
                Snackbar.make(binding.root, "Enter a search query", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.searchResults.observe(viewLifecycleOwner) { books ->
            adapter.submitList(books)
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.loading.isVisible = isLoading
            if (isLoading) {
                binding.doSearchBtn.isEnabled = false
            } else {
                binding.doSearchBtn.postDelayed({
                    _binding?.let { it.doSearchBtn.isEnabled = true }
                }, 1000)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
