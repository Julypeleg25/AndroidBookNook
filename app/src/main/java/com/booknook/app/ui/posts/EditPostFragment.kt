package com.booknook.app.ui.posts

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.booknook.app.R
import com.booknook.app.base.MyApplication
import com.booknook.app.databinding.FragmentCreatePostBinding
import com.booknook.app.util.IMAGE_PICKER_MIME_TYPE
import com.booknook.app.util.loadLocalImage
import com.booknook.app.util.loadRemoteImage
import com.booknook.app.util.nullIfBlank
import com.google.android.material.snackbar.Snackbar

class EditPostFragment : Fragment(R.layout.fragment_create_post) {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as MyApplication
    private val viewModel: PostEditorViewModel by viewModels {
        PostEditorViewModel.factory(
            postsRepository = app.postsRepository,
            authRepository = app.authRepository
        )
    }
    private var pickedImage: Uri? = null
    private lateinit var postId: String
    private var hasInitializedForm = false
    private var initialRating = 0
    private var initialReview = ""

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pickedImage = it
            binding.imageCard.isVisible = true
            binding.imageRequiredHint.isVisible = false
            binding.imagePreview.loadLocalImage(it, R.drawable.book_placeholder)
            updateSaveButton()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreatePostBinding.bind(view)
        postId = EditPostFragmentArgs.fromBundle(requireArguments()).postId

        binding.publishBtn.text = getString(R.string.edit_post_save_button)
        binding.pickImageBtn.text = getString(R.string.edit_post_replace_image_button)
        binding.bookHeader.text = getString(R.string.edit_post_book_header)
        binding.bookSubtitle.text = getString(R.string.edit_post_book_subtitle)
        binding.imageRequiredHint.isVisible = false
        binding.publishBtn.isEnabled = false

        observeViewModel()
        viewModel.loadPost(postId)

        binding.pickImageBtn.setOnClickListener { pickImage.launch(IMAGE_PICKER_MIME_TYPE) }
        binding.ratingBar.setOnRatingBarChangeListener { _, _, _ ->
            updateSaveButton()
        }
        binding.reviewInput.doAfterTextChanged {
            updateSaveButton()
        }

        binding.publishBtn.setOnClickListener {
            val rating = binding.ratingBar.rating.toInt()
            val review = binding.reviewInput.text.toString().trim()
            if (binding.ratingBar.rating == 0f || review.isEmpty()) {
                Snackbar.make(binding.root, R.string.create_post_rating_required, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.updatePost(postId, rating, review, pickedImage)
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val isBusy = state.isSaving || state.isLoadingPost
            binding.loading.isVisible = isBusy
            updateSaveButton(isBusy)

            val post = state.post ?: return@observe

            if (!hasInitializedForm) {
                binding.ratingBar.rating = post.rating.toFloat()
                binding.reviewInput.setText(post.review)
                initialRating = post.rating
                initialReview = post.review.trim()

                BookInfoCardBinder.bind(
                    binding.bookInfoPanel,
                    BookInfoCardModel(
                        title = post.bookTitle,
                        author = post.bookAuthor,
                        thumbnail = post.bookThumbnail,
                        genre = post.bookGenre,
                        publishedDate = post.bookPublishedDate,
                        pageCount = post.bookPageCount,
                        description = post.bookDescription
                    )
                )
                hasInitializedForm = true
                updateSaveButton(isBusy)
            }

            val existingImageUrl = post.imageUrl.nullIfBlank()
            if (pickedImage == null && existingImageUrl != null) {
                binding.imageCard.isVisible = true
                binding.imagePreview.loadRemoteImage(existingImageUrl, R.drawable.book_placeholder)
            }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { action ->
                when (action) {
                    is PostEditorEvent.Finish -> {
                        Snackbar.make(binding.root, action.messageRes, Snackbar.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }

                    is PostEditorEvent.ShowMessage -> {
                        Snackbar.make(binding.root, getString(action.messageRes), Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateSaveButton(isBusy: Boolean = viewModel.uiState.value?.isSaving == true || viewModel.uiState.value?.isLoadingPost == true) {
        binding.publishBtn.isEnabled = !isBusy && hasInitializedForm && hasFormChanged()
    }

    private fun hasFormChanged(): Boolean {
        val currentRating = binding.ratingBar.rating.toInt()
        val currentReview = binding.reviewInput.text?.toString()?.trim().orEmpty()
        return currentRating != initialRating || currentReview != initialReview || pickedImage != null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
