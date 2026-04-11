package com.booknook.app.ui.posts

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.booknook.app.R
import com.booknook.app.base.MyApplication
import com.booknook.app.databinding.FragmentPostDetailsBinding
import com.booknook.app.util.formatRelativeTime
import com.booknook.app.util.loadRemoteImage
import com.booknook.app.util.nullIfBlank
import com.google.android.material.snackbar.Snackbar

class PostDetailsFragment : Fragment(R.layout.fragment_post_details) {

    private var _binding: FragmentPostDetailsBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as MyApplication
    private val viewModel: PostDetailsViewModel by viewModels {
        PostDetailsViewModel.factory(
            postsRepository = app.postsRepository,
            booksRepository = app.booksRepository,
            listsRepository = app.listsRepository,
            authRepository = app.authRepository
        )
    }
    private val commentsAdapter = CommentsAdapter()
    private var lastBoundBookInfo: BookInfoCardModel? = null
    private lateinit var postId: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPostDetailsBinding.bind(view)
        postId = PostDetailsFragmentArgs.fromBundle(requireArguments()).postId

        binding.commentsRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.commentsRecyclerView.adapter = commentsAdapter
        binding.commentsRecyclerView.itemAnimator = null

        observeViewModel()
        viewModel.loadPost(postId)

        binding.likeBtn.setOnClickListener {
            viewModel.onLikeClicked()
        }

        binding.addCommentBtn.setOnClickListener {
            viewModel.onAddCommentSubmitted(binding.commentInput.text.toString().trim())
        }

        binding.addWishlistBtn.setOnClickListener {
            viewModel.onWishlistClicked()
        }

        binding.addReadlistBtn.setOnClickListener {
            viewModel.onReadlistClicked()
        }

        binding.editBtn.setOnClickListener {
            viewModel.onEditRequested()
        }

        binding.title.setOnLongClickListener {
            viewModel.onEditRequested()
            true
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.pbAction.isVisible = state.isActionProcessing
            binding.addCommentBtn.isVisible = !state.isCommentProcessing
            binding.pbComment.isVisible = state.isCommentProcessing
            binding.bookDetailsHeader.isVisible = state.bookInfo != null || state.post != null

            binding.addWishlistBtn.isEnabled = !state.isActionProcessing
            binding.addReadlistBtn.isEnabled = !state.isActionProcessing
            binding.addWishlistBtn.setIconResource(
                if (state.isWishlisted) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
            )
            binding.addReadlistBtn.setIconResource(
                if (state.isReadlisted) R.drawable.ic_check_circle_filled else R.drawable.ic_check_circle_outline
            )

            commentsAdapter.submitList(state.comments)
            if (state.bookInfo == null) {
                lastBoundBookInfo = null
            } else if (state.bookInfo != lastBoundBookInfo) {
                val bookInfo = state.bookInfo
                BookInfoCardBinder.bind(binding.bookInfoPanel, bookInfo)
                lastBoundBookInfo = bookInfo
            }

            state.post?.let { post ->
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
                binding.likeBtn.setIconResource(
                    if (post.isLikedByUser) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                )

                binding.editBtn.isVisible = state.canEdit

                val canLike = !state.isActionProcessing && !state.canEdit
                binding.likeBtn.isEnabled = canLike
                binding.likeBtn.alpha = if (canLike) 1.0f else 0.5f

                binding.postImage.loadRemoteImage(
                    post.imageUrl.nullIfBlank() ?: post.bookThumbnail,
                    R.drawable.book_placeholder
                )
            }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { action ->
                when (action) {
                    is PostDetailsEvent.NavigateToEdit -> {
                        val direction = PostDetailsFragmentDirections.actionDetailsToEdit(action.postId)
                        findNavController().navigate(direction)
                    }

                    is PostDetailsEvent.CommentAdded -> {
                        binding.commentInput.setText("")
                        Snackbar.make(binding.root, getString(action.messageRes), Snackbar.LENGTH_SHORT).show()
                    }

                    is PostDetailsEvent.ShowMessage -> {
                        Snackbar.make(binding.root, getString(action.messageRes), Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.commentsRecyclerView.adapter = null
        lastBoundBookInfo = null
        super.onDestroyView()
        _binding = null
    }
}
