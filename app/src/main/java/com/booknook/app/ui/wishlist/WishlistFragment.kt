package com.booknook.app.ui.wishlist

import androidx.lifecycle.lifecycleScope
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.booknook.app.R
import com.booknook.app.databinding.FragmentWishlistBinding
import com.booknook.app.model.Model
import kotlinx.coroutines.launch

class WishlistFragment : Fragment(R.layout.fragment_wishlist) {

    private lateinit var binding: FragmentWishlistBinding
    private lateinit var wishlistAdapter: WishlistAdapter
    private lateinit var readlistAdapter: ReadlistAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentWishlistBinding.bind(view)

        val uid = Model.currentUserId() ?: return

        wishlistAdapter = WishlistAdapter { item ->
            viewLifecycleOwner.lifecycleScope.launch { Model.removeFromWishlist(uid, item.bookId) }
            Toast.makeText(requireContext(), "Removed from wishlist", Toast.LENGTH_SHORT).show()
        }

        readlistAdapter = ReadlistAdapter { item ->
            viewLifecycleOwner.lifecycleScope.launch { Model.removeFromReadlist(uid, item.bookId) }
            Toast.makeText(requireContext(), "Removed from readlist", Toast.LENGTH_SHORT).show()
        }

        binding.wishlistRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.wishlistRecycler.adapter = wishlistAdapter

        binding.readlistRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.readlistRecycler.adapter = readlistAdapter

        Model.observeWishlist(uid).observe(viewLifecycleOwner) { list ->
            wishlistAdapter.submit(list)
        }

        Model.observeReadlist(uid).observe(viewLifecycleOwner) { list ->
            readlistAdapter.submit(list)
        }
    }
}
