package com.booknook.app.ui.lists

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.booknook.app.R
import com.booknook.app.base.MyApplication
import com.booknook.app.databinding.FragmentMyListsBinding
import com.google.android.material.snackbar.Snackbar

class MyListsFragment : Fragment(R.layout.fragment_my_lists) {

    private var _binding: FragmentMyListsBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as MyApplication
    private val viewModel: MyListsViewModel by viewModels {
        MyListsViewModel.factory(
            listsRepository = app.listsRepository,
            authRepository = app.authRepository
        )
    }
    private lateinit var wishlistAdapter: SavedBooksAdapter
    private lateinit var readlistAdapter: SavedBooksAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyListsBinding.bind(view)

        setupAdapters()
        observeViewModel()
    }

    private fun setupAdapters() {
        wishlistAdapter = SavedBooksAdapter { item ->
            viewModel.onRemoveFromWishlist(item.bookId)
        }

        readlistAdapter = SavedBooksAdapter { item ->
            viewModel.onRemoveFromReadlist(item.bookId)
        }

        binding.wishlistRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.wishlistRecycler.adapter = wishlistAdapter

        binding.readlistRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.readlistRecycler.adapter = readlistAdapter
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            wishlistAdapter.submitList(state.wishlist)
            readlistAdapter.submitList(state.readlist)
            binding.emptyWishlist.isVisible = state.wishlist.isEmpty()
            binding.emptyReadlist.isVisible = state.readlist.isEmpty()
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { action ->
                when (action) {
                    is MyListsEvent.ShowMessage -> {
                        Snackbar.make(binding.root, getString(action.messageRes), Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.wishlistRecycler.adapter = null
        binding.readlistRecycler.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
