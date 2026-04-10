package com.booknook.app.ui.books

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.booknook.app.R
import com.booknook.app.databinding.FragmentBookSearchBinding
import com.google.android.material.snackbar.Snackbar

class BookSearchFragment : Fragment(R.layout.fragment_book_search) {

    private var _binding: FragmentBookSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookSearchViewModel by viewModels()
    private var hasSearched = false

    private val adapter = BookAdapter { book ->
        val action = BookSearchFragmentDirections.actionBookSearchToCreatePost(
            bookId = book.id,
            bookTitle = book.title,
            bookAuthor = book.author,
            bookThumbnail = book.thumbnail,
            bookPublishedDate = book.publishedDate,
            bookGenre = book.genre,
            bookPageCount = book.pageCount ?: -1,
            bookDescription = book.description
        )
        findNavController().navigate(action)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBookSearchBinding.bind(view)

        val layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.layoutManager = layoutManager
        binding.recycler.adapter = adapter

        binding.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                        viewModel.loadMore()
                    }
                }
            }
        })

        observeViewModel()
        binding.doSearchBtn.setOnClickListener { performSearch() }
        binding.clearSearchBtn.setOnClickListener { clearSearchUi() }

        binding.queryInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else false
        }

        binding.queryInput.doAfterTextChanged { text ->
            val hasText = !text.isNullOrBlank()
            binding.clearSearchBtn.isVisible = hasText || hasSearched
            if (!hasText && hasSearched) {
                clearSearchUi()
            }
        }
    }

    private fun performSearch() {
        val q = binding.queryInput.text.toString().trim()
        if (q.isNotEmpty()) {
            hasSearched = true
            binding.welcomeGroup.isVisible = false
            viewModel.searchBooks(q)
        } else {
            Snackbar.make(binding.root, R.string.search_query_required, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.searchResults.observe(viewLifecycleOwner) { books ->
            adapter.submitList(books)
            binding.recycler.isVisible = books.isNotEmpty()
            binding.welcomeGroup.isVisible = !hasSearched
            binding.emptyText.isVisible = hasSearched && books.isEmpty() && viewModel.loading.value != true
            binding.clearSearchBtn.isVisible = binding.queryInput.text?.isNotBlank() == true || hasSearched
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.loading.isVisible = isLoading
            if (isLoading) {
                binding.welcomeGroup.isVisible = false
                binding.emptyText.isVisible = false
            }
        }

        viewModel.loadingMore.observe(viewLifecycleOwner) { isLoadingMore ->
            binding.pbLoadingMore.isVisible = isLoadingMore
        }

        viewModel.isEmpty.observe(viewLifecycleOwner) { empty ->
            binding.emptyText.isVisible = empty && hasSearched
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, getString(it), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun clearSearchUi() {
        viewModel.clearSearch()
        hasSearched = false
        binding.queryInput.setText("")
        binding.recycler.isVisible = false
        binding.emptyText.isVisible = false
        binding.welcomeGroup.isVisible = true
        binding.clearSearchBtn.isVisible = false
    }
}
