package com.booknook.app.ui.posts

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.navigation.fragment.findNavController
import com.booknook.app.R
import com.booknook.app.databinding.FragmentPostDetailsBinding
import com.booknook.app.model.Model
import com.booknook.app.util.formatRelativeTime
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso

class PostDetailsFragment : Fragment(R.layout.fragment_post_details) {

    private var _binding: FragmentPostDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PostDetailsViewModel by viewModels()
    private val commentsAdapter = CommentsAdapter()
    private lateinit var postId: String
    private var observedBookId: String? = null
    private var wishlistState: LiveData<Boolean>? = null
    private var readlistState: LiveData<Boolean>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPostDetailsBinding.bind(view)
        postId = PostDetailsFragmentArgs.fromBundle(requireArguments()).postId

        binding.commentsRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.commentsRecyclerView.adapter = commentsAdapter

        observeViewModel()
        viewModel.refreshComments(postId)

        binding.likeBtn.setOnClickListener {
            viewModel.toggleLike(postId)
        }

        binding.addCommentBtn.setOnClickListener {
            val text = binding.commentInput.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.addComment(postId, text)
                binding.commentInput.setText("")
            }
        }

        binding.addWishlistBtn.setOnClickListener {
            viewModel.toggleWishlist(postId)
        }

        binding.addReadlistBtn.setOnClickListener {
            viewModel.toggleReadlist(postId)
        }

        binding.title.setOnLongClickListener {
            val action = PostDetailsFragmentDirections.actionDetailsToEdit(postId)
            findNavController().navigate(action)
            true
        }
    }

    private fun observeViewModel() {
        viewModel.isActionProcessing.observe(viewLifecycleOwner) { isProcessing ->
            binding.likeBtn.isEnabled = !isProcessing
            binding.addWishlistBtn.isEnabled = !isProcessing
            binding.addReadlistBtn.isEnabled = !isProcessing
        }

        viewModel.isCommentProcessing.observe(viewLifecycleOwner) { isProcessing ->
            binding.addCommentBtn.isEnabled = !isProcessing
        }

        viewModel.observeComments(postId).observe(viewLifecycleOwner) { comments ->
            commentsAdapter.submitList(comments)
        }

        viewModel.bookInfo.observe(viewLifecycleOwner) { bookInfo ->
            BookInfoCardBinder.bind(binding.bookInfoPanel, bookInfo)
        }

        viewModel.observePost(postId).observe(viewLifecycleOwner) { post ->
            if (post == null) return@observe

            binding.title.text = post.bookTitle
            binding.author.text = post.bookAuthor
            binding.rating.rating = post.rating.toFloat()
            binding.review.text = post.review
            binding.meta.text = getString(
                R.string.post_meta_format,
                post.username,
                requireContext().formatRelativeTime(post.createdAt)
            )
            binding.likeBtn.text = getString(R.string.post_like_button_format, post.likesCount)

            binding.bookDetailsHeader.isVisible = true
            viewModel.resolveBookInfo(post)

            val isOwnPost = post.userId == Model.authRepository.currentUserId()
            val likeIcon = if (post.isLikedByUser) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            binding.likeBtn.setIconResource(likeIcon)
            
            if (isOwnPost) {
                binding.likeBtn.isEnabled = false
                binding.likeBtn.alpha = 0.5f
            } else {
                binding.likeBtn.alpha = 1.0f
            }
            bindBookListStates(post.bookId)

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
                Snackbar.make(binding.root, getString(it), Snackbar.LENGTH_SHORT).show()
            }
        }

        viewModel.actionFeedback.observe(viewLifecycleOwner) { feedback ->
            feedback?.let {
                Snackbar.make(binding.root, getString(it), Snackbar.LENGTH_SHORT).show()
                viewModel.resetFeedback()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bindBookListStates(bookId: String) {
        if (observedBookId == bookId) return

        wishlistState?.removeObservers(viewLifecycleOwner)
        readlistState?.removeObservers(viewLifecycleOwner)
        observedBookId = bookId

        wishlistState = viewModel.observeWishlist(bookId).also { liveData ->
            liveData.observe(viewLifecycleOwner) { isWishlisted ->
                val icon = if (isWishlisted) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
                binding.addWishlistBtn.setIconResource(icon)
            }
        }

        readlistState = viewModel.observeReadlist(bookId).also { liveData ->
            liveData.observe(viewLifecycleOwner) { isReadlisted ->
                val icon = if (isReadlisted) R.drawable.ic_check_circle_filled else R.drawable.ic_check_circle_outline
                binding.addReadlistBtn.setIconResource(icon)
            }
        }
    }

}
