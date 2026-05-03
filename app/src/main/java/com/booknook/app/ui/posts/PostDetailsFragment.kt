package com.booknook.app.ui.posts

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.booknook.app.R
import com.booknook.app.base.MyApplication
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.databinding.FragmentPostDetailsBinding
import com.booknook.app.util.displayPostImageUrl
import com.booknook.app.util.formatRelativeTime
import com.booknook.app.util.loadRemoteImage
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

        setupCommentsList()
        setupActions()
        observeViewModel()
        viewModel.loadPost(postId)
    }

    private fun setupCommentsList() {
        binding.commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.commentsRecyclerView.adapter = commentsAdapter
        binding.commentsRecyclerView.itemAnimator = null
    }

    private fun setupActions() {
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
            renderState(state)
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { action ->
                handleEvent(action)
            }
        }
    }

    private fun renderState(state: PostDetailsUiState) {
        renderLoadingState(state)
        renderListButtons(state)
        commentsAdapter.submitList(state.comments)
        renderBookInfo(state.bookInfo)
        state.post?.let { post -> renderPost(post, state) }
    }

    private fun renderLoadingState(state: PostDetailsUiState) {
        binding.pbAction.isVisible = state.isActionProcessing
        binding.addCommentBtn.isVisible = !state.isCommentProcessing
        binding.pbComment.isVisible = state.isCommentProcessing
        binding.bookDetailsHeader.isVisible = state.bookInfo != null || state.post != null
    }

    private fun renderListButtons(state: PostDetailsUiState) {
        binding.addWishlistBtn.isEnabled = !state.isActionProcessing
        binding.addReadlistBtn.isEnabled = !state.isActionProcessing
        binding.addWishlistBtn.setIconResource(state.wishlistIconRes)
        binding.addReadlistBtn.setIconResource(state.readlistIconRes)
    }

    private fun renderBookInfo(bookInfo: BookInfoCardModel?) {
        if (bookInfo == null) {
            lastBoundBookInfo = null
            return
        }
        if (bookInfo == lastBoundBookInfo) return

        BookInfoCardBinder.bind(binding.bookInfoPanel, bookInfo)
        lastBoundBookInfo = bookInfo
    }

    private fun renderPost(post: PostEntity, state: PostDetailsUiState) {
        binding.title.text = post.bookTitle
        binding.author.text = post.bookAuthor
        binding.rating.rating = post.rating.toFloat()
        binding.review.text = post.review
        binding.meta.text = post.metaText
        binding.likeBtn.text = getString(R.string.post_like_button_format, post.likesCount)
        binding.likeBtn.setIconResource(post.likeIconRes)
        binding.editBtn.isVisible = state.canEdit
        renderLikeButton(state)
        binding.postImage.loadRemoteImage(post.displayImageUrl, R.drawable.book_placeholder)
    }

    private fun renderLikeButton(state: PostDetailsUiState) {
        val canLike = !state.isActionProcessing && !state.canEdit
        binding.likeBtn.isEnabled = canLike
        binding.likeBtn.alpha = if (canLike) ENABLED_ALPHA else DISABLED_ALPHA
    }

    private fun handleEvent(event: PostDetailsEvent) {
        when (event) {
            is PostDetailsEvent.NavigateToEdit -> navigateToEdit(event.postId)
            is PostDetailsEvent.CommentAdded -> handleCommentAdded(event.messageRes)
            is PostDetailsEvent.ShowMessage -> showMessage(event.messageRes)
        }
    }

    private fun navigateToEdit(postId: String) {
        val direction = PostDetailsFragmentDirections.actionDetailsToEdit(postId)
        findNavController().navigate(direction)
    }

    private fun handleCommentAdded(messageRes: Int) {
        binding.commentInput.setText("")
        showMessage(messageRes)
    }

    private fun showMessage(messageRes: Int) {
        Snackbar.make(binding.root, getString(messageRes), Snackbar.LENGTH_SHORT).show()
    }

    private val PostDetailsUiState.wishlistIconRes: Int
        get() = if (isWishlisted) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline

    private val PostDetailsUiState.readlistIconRes: Int
        get() = if (isReadlisted) R.drawable.ic_check_circle_filled else R.drawable.ic_check_circle_outline

    private val PostEntity.likeIconRes: Int
        get() = if (isLikedByUser) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline

    private val PostEntity.displayImageUrl: String?
        get() = displayPostImageUrl

    private val PostEntity.metaText: String
        get() = getString(
            R.string.post_meta_format,
            username,
            requireContext().formatRelativeTime(createdAt)
        )

    override fun onDestroyView() {
        binding.commentsRecyclerView.adapter = null
        lastBoundBookInfo = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ENABLED_ALPHA = 1.0f
        private const val DISABLED_ALPHA = 0.5f
    }
}
