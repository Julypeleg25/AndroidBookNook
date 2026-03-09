package com.booknook.app.ui.posts

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.booknook.app.R
import com.booknook.app.databinding.FragmentFiltersBinding

class FiltersFragment : Fragment(R.layout.fragment_filters) {

    private var _binding: FragmentFiltersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FiltersViewModel by viewModels()
    
    private val resultsAdapter = PostsAdapter(
        onClick = { postId ->
            val action = FiltersFragmentDirections.actionFiltersToDetails(postId)
            findNavController().navigate(action)
        }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFiltersBinding.bind(view)

        binding.resultsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.resultsRecycler.adapter = resultsAdapter

        observeViewModel()

        binding.applyBtn.setOnClickListener {
            val title = binding.titleInput.text.toString().trim().ifEmpty { null }
            val author = binding.authorInput.text.toString().trim().ifEmpty { null }
            val minRating = binding.minRatingInput.text.toString().trim().toIntOrNull()
            val minComments = binding.minCommentsInput.text.toString().trim().toIntOrNull()

            viewModel.applyFilters(title, author, minRating, minComments)
        }
    }

    private fun observeViewModel() {
        viewModel.filteredPosts.observe(viewLifecycleOwner) { list ->
            resultsAdapter.submitList(list)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
