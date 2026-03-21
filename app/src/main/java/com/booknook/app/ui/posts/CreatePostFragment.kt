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
import com.booknook.app.domain.Book
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso

class CreatePostFragment : Fragment(R.layout.fragment_create_post) {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CreatePostViewModel by viewModels()
    private var pickedImage: Uri? = null

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

        val args = CreatePostFragmentArgs.fromBundle(requireArguments())
        val selectedBook = Book(
            id = args.bookId,
            title = args.bookTitle,
            author = args.bookAuthor,
            thumbnail = args.bookThumbnail,
            publishedDate = args.bookPublishedDate,
            genre = args.bookGenre,
            pageCount = if (args.bookPageCount > 0) args.bookPageCount else null,
            description = args.bookDescription
        )

        BookInfoCardBinder.bind(
            binding.bookInfoPanel,
            BookInfoCardModel(
                title = selectedBook.title,
                author = selectedBook.author,
                thumbnail = selectedBook.thumbnail,
                genre = selectedBook.genre,
                publishedDate = selectedBook.publishedDate,
                pageCount = selectedBook.pageCount,
                description = selectedBook.description
            )
        )

        observeViewModel()

        binding.pickImageBtn.setOnClickListener { pickImage.launch("image/*") }

        binding.publishBtn.setOnClickListener {
            val rating = binding.ratingBar.rating.toInt()
            val review = binding.reviewInput.text.toString().trim()
            if (binding.ratingBar.rating == 0f || review.isEmpty()) {
                Snackbar.make(binding.root, R.string.create_post_rating_required, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pickedImage == null) {
                Snackbar.make(binding.root, R.string.create_post_photo_required, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.createPost(selectedBook, rating, review, pickedImage)
        }
    }

    private fun observeViewModel() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.loading.isVisible = isLoading
            binding.publishBtn.isEnabled = !isLoading
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { success ->
                if (success) {
                    com.booknook.app.util.Logger.d("CreatePost", "Consuming success event - Navigating back")
                    Snackbar.make(binding.root, R.string.create_post_success, Snackbar.LENGTH_SHORT).show()
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
