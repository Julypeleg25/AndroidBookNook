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
import com.booknook.app.model.Book
import com.google.android.material.snackbar.Snackbar

class BookSearchFragment : Fragment(R.layout.fragment_book_search) {

    private var _binding: FragmentBookSearchBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as MyApplication
    private val viewModel: BookSearchViewModel by viewModels {
        BookSearchViewModel.factory(
            booksRepository = app.booksRepository,
            authRepository = app.authRepository
        )
    }

    private val adapter = BookAdapter { book ->
        viewModel.onBookSelected(book)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBookSearchBinding.bind(view)

        setupResultsList()
        setupActions()
        observeViewModel()
    }

    private fun setupResultsList() {
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
        binding.recycler.addOnScrollListener(createLoadMoreScrollListener())
    }

    private fun createLoadMoreScrollListener(): RecyclerView.OnScrollListener {
        val layoutManager = binding.recycler.layoutManager as LinearLayoutManager
        return object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && layoutManager.isNearEnd()) {
                    viewModel.loadMore()
                }
            }
        }
    }

    private fun setupActions() {
        binding.doSearchBtn.setOnClickListener {
            viewModel.onSearchRequested()
        }
        binding.clearSearchBtn.setOnClickListener {
            viewModel.clearSearch()
        }
        binding.queryInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.onSearchRequested()
                true
            } else {
                false
            }
        }
        binding.queryInput.doAfterTextChanged { text ->
            viewModel.onQueryChanged(text?.toString().orEmpty())
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { action ->
                handleEvent(action)
            }
        }
    }

    private fun renderState(state: BookSearchUiState) {
        updateQueryInput(state.query)
        adapter.submitList(state.results)
        binding.recycler.isVisible = state.results.isNotEmpty()
        binding.loading.isVisible = state.isInitialLoading
        binding.pbLoadingMore.isVisible = state.isLoadingMore
        binding.welcomeGroup.isVisible = !state.hasSearched && !state.isInitialLoading
        binding.emptyText.isVisible = state.hasSearched && state.isEmpty && !state.isInitialLoading
        binding.clearSearchBtn.isVisible = state.query.isNotBlank() || state.hasSearched
    }

    private fun updateQueryInput(query: String) {
        if (binding.queryInput.text?.toString() == query) return
        binding.queryInput.setText(query)
        binding.queryInput.setSelection(query.length)
    }

    private fun handleEvent(event: BookSearchEvent) {
        when (event) {
            is BookSearchEvent.NavigateToCreatePost -> navigateToCreatePost(event.book)
            is BookSearchEvent.ShowMessage -> showMessage(event.messageRes)
        }
    }

    private fun navigateToCreatePost(book: Book) {
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

    private fun showMessage(messageRes: Int) {
        Snackbar.make(binding.root, getString(messageRes), Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        binding.recycler.adapter = null
        super.onDestroyView()
        _binding = null
    }

    private fun LinearLayoutManager.isNearEnd(): Boolean {
        val firstVisiblePosition = findFirstVisibleItemPosition()
        return firstVisiblePosition >= 0 &&
            childCount + firstVisiblePosition >= itemCount - LOAD_MORE_THRESHOLD
    }

    companion object {
        private const val LOAD_MORE_THRESHOLD = 5
    }
}
