package com.booknook.app.ui.posts

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.booknook.app.R
import com.booknook.app.databinding.FragmentPostDetailsBinding
import com.booknook.app.model.Model
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso

class PostDetailsFragment : Fragment(R.layout.fragment_post_details) {

    private var _binding: FragmentPostDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PostDetailsViewModel by viewModels()
    private lateinit var postId: String
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPostDetailsBinding.bind(view)
        postId = PostDetailsFragmentArgs.fromBundle(requireArguments()).postId

        observeViewModel()

        binding.likeBtn.setOnClickListener {
            viewModel.toggleLike(postId)
        }

        binding.addCommentBtn.setOnClickListener {
            val text = binding.commentInput.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            viewModel.addComment(postId, text)
            binding.commentInput.setText("")
        }

        binding.addWishlistBtn.setOnClickListener {
            viewModel.addToWishlist(postId)
        }

        binding.addReadlistBtn.setOnClickListener {
            viewModel.addToReadlist(postId)
        }

        binding.title.setOnLongClickListener {
            val action = PostDetailsFragmentDirections.actionDetailsToEdit(postId)
            findNavController().navigate(action)
            true
        }
    }

    private fun observeViewModel() {
        viewModel.observePost(postId).observe(viewLifecycleOwner) { post ->
            if (post == null) return@observe

            binding.title.text = post.bookTitle
            binding.author.text = post.bookAuthor
            binding.rating.rating = post.rating.toFloat()
            binding.review.text = post.review
            binding.meta.text = "${post.likesCount} likes • ${post.commentsCount} comments"

            Picasso.get()
                .load(post.imageUrl ?: post.bookThumbnail)
                .placeholder(R.drawable.book_placeholder)
                .error(R.drawable.book_placeholder)
                .fit()
                .centerCrop()
                .into(binding.postImage)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
            }
        }

        viewModel.actionFeedback.observe(viewLifecycleOwner) { feedback ->
            feedback?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                viewModel.resetFeedback()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
