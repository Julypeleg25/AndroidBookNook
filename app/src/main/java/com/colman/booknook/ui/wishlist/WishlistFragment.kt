package com.colman.booknook.ui.wishlist

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.colman.booknook.R
import com.colman.booknook.databinding.FragmentWishlistBinding
import com.colman.booknook.model.Model
import kotlinx.coroutines.launch

class WishlistFragment : Fragment(R.layout.fragment_wishlist) {

    private lateinit var binding: FragmentWishlistBinding
    private lateinit var adapter: WishlistAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentWishlistBinding.bind(view)

        val uid = Model.currentUserId() ?: return

        adapter = WishlistAdapter { item ->
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    Model.removeFromWishlist(uid, item.bookId)
                    Toast.makeText(requireContext(), "Removed", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), e.message ?: "Remove failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        Model.observeWishlist(uid).observe(viewLifecycleOwner) { list ->
            adapter.submit(list)
        }
    }
}
