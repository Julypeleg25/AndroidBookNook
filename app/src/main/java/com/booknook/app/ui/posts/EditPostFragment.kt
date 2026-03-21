package com.booknook.app.ui.posts

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.booknook.app.R
import com.booknook.app.databinding.FragmentCreatePostBinding
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso

class EditPostFragment : Fragment(R.layout.fragment_create_post) {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CreatePostViewModel by viewModels()
    private var pickedImage: Uri? = null
    private lateinit var postId: String
    private var hasInitializedForm = false

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pickedImage = it
            binding.imageCard.isVisible = true
            binding.imageRequiredHint.isVisible = false
            Picasso.get().load(it).fit().centerCrop().into(binding.imagePreview)
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

        observeViewModel()

        binding.pickImageBtn.setOnClickListener { pickImage.launch("image/*") }

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
        viewModel.observePost(postId).observe(viewLifecycleOwner) { post ->
            if (post == null) return@observe

            if (!hasInitializedForm) {
                binding.ratingBar.rating = post.rating.toFloat()
                binding.reviewInput.setText(post.review)

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
            }

            if (pickedImage == null && !post.imageUrl.isNullOrBlank()) {
                binding.imageCard.isVisible = true
                Picasso.get().load(post.imageUrl).fit().centerCrop().into(binding.imagePreview)
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.loading.isVisible = isLoading
            binding.publishBtn.isEnabled = !isLoading
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { success ->
                if (success) {
                    Snackbar.make(binding.root, R.string.create_post_update_success, Snackbar.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, getString(it), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
