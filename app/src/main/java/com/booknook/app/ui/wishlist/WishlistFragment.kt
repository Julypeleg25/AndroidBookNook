package com.booknook.app.ui.wishlist

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.booknook.app.R
import com.booknook.app.databinding.FragmentWishlistBinding
import com.google.android.material.snackbar.Snackbar

class WishlistFragment : Fragment(R.layout.fragment_wishlist) {

    private var _binding: FragmentWishlistBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WishlistViewModel by viewModels()
    private lateinit var wishlistAdapter: WishlistAdapter
    private lateinit var readlistAdapter: ReadlistAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWishlistBinding.bind(view)

        setupAdapters()
        observeViewModel()
    }

    private fun setupAdapters() {
        wishlistAdapter = WishlistAdapter { item ->
            viewModel.removeFromWishlist(item.bookId)
        }

        readlistAdapter = ReadlistAdapter { item ->
            viewModel.removeFromReadlist(item.bookId)
        }

        binding.wishlistRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.wishlistRecycler.adapter = wishlistAdapter

        binding.readlistRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.readlistRecycler.adapter = readlistAdapter
    }

    private fun observeViewModel() {
        viewModel.observeWishlist().observe(viewLifecycleOwner) { list ->
            wishlistAdapter.submitList(list)
            binding.emptyWishlist.isVisible = list.isEmpty()
        }

        viewModel.observeReadlist().observe(viewLifecycleOwner) { list ->
            readlistAdapter.submitList(list)
            binding.emptyReadlist.isVisible = list.isEmpty()
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, getString(it), Snackbar.LENGTH_SHORT).show()
            }
        }

        viewModel.removeFeedback.observe(viewLifecycleOwner) { messageRes ->
            messageRes?.let {
                Snackbar.make(binding.root, getString(it), Snackbar.LENGTH_SHORT).show()
                viewModel.resetRemoveFeedback()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
