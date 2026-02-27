package com.colman.booknook.ui.posts

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.colman.booknook.R
import com.colman.booknook.databinding.FragmentFiltersBinding
import com.colman.booknook.model.Model

class FiltersFragment : Fragment(R.layout.fragment_filters) {

    private lateinit var binding: FragmentFiltersBinding
    private val resultsAdapter = PostsAdapter { postId ->
        findNavController().navigate(
            R.id.postDetailsFragment,
            bundleOf("postId" to postId)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentFiltersBinding.bind(view)

        binding.resultsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.resultsRecycler.adapter = resultsAdapter

        binding.applyBtn.setOnClickListener {
            val title = binding.titleInput.text.toString().trim().ifEmpty { null }
            val author = binding.authorInput.text.toString().trim().ifEmpty { null }
            val minRating = binding.minRatingInput.text.toString().trim().toIntOrNull()
            val minComments = binding.minCommentsInput.text.toString().trim().toIntOrNull()

            Model.searchPosts(title, author, minRating, minComments).observe(viewLifecycleOwner) { list ->
                resultsAdapter.submit(list)
            }
        }
    }
}
