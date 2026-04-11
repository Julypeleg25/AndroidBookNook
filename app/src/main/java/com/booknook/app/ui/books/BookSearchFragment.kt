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
import com.booknook.app.base.MyApplication
import com.booknook.app.databinding.FragmentBookSearchBinding
import com.google.android.material.snackbar.Snackbar

class BookSearchFragment : Fragment(R.layout.fragment_book_search) {

    private var _binding: FragmentBookSearchBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as MyApplication
    private val viewModel: BookSearchViewModel by viewModels {
        BookSearchViewModel.factory(app.booksRepository)
    }


    private val adapter = BookAdapter { book ->
        viewModel.onBookSelected(book)
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
        binding.doSearchBtn.setOnClickListener { viewModel.onSearchRequested() }
        binding.clearSearchBtn.setOnClickListener { viewModel.clearSearch() }

        binding.queryInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.onSearchRequested()
                true
            } else false
        }

        binding.queryInput.doAfterTextChanged { text ->
            viewModel.onQueryChanged(text?.toString().orEmpty())
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (binding.queryInput.text?.toString() != state.query) {
                binding.queryInput.setText(state.query)
                binding.queryInput.setSelection(state.query.length)
            }

            adapter.submitList(state.results)
            binding.recycler.isVisible = state.results.isNotEmpty()
            binding.loading.isVisible = state.isInitialLoading
            binding.pbLoadingMore.isVisible = state.isLoadingMore
            binding.welcomeGroup.isVisible = !state.hasSearched && !state.isInitialLoading
            binding.emptyText.isVisible = state.hasSearched && state.isEmpty && !state.isInitialLoading
            binding.clearSearchBtn.isVisible = state.query.isNotBlank() || state.hasSearched
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { action ->
                when (action) {
                    is BookSearchEvent.NavigateToCreatePost -> {
                        val book = action.book
                        val direction = BookSearchFragmentDirections.actionBookSearchToCreatePost(
                            bookId = book.id,
                            bookTitle = book.title,
                            bookAuthor = book.author,
                            bookThumbnail = book.thumbnail,
                            bookPublishedDate = book.publishedDate,
                            bookGenre = book.genre,
                            bookPageCount = book.pageCount ?: -1,
                            bookDescription = book.description
                        )
                        findNavController().navigate(direction)
                    }

                    is BookSearchEvent.ShowMessage -> {
                        Snackbar.make(binding.root, getString(action.messageRes), Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.recycler.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
