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
            binding.imagePreview.isVisible = true
            Picasso.get().load(it).fit().centerCrop().into(binding.imagePreview)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreatePostBinding.bind(view)

        val args = CreatePostFragmentArgs.fromBundle(requireArguments())
        binding.bookTitle.text = args.bookTitle
        binding.bookAuthor.text = args.bookAuthor

        observeViewModel()

        binding.pickImageBtn.setOnClickListener { pickImage.launch("image/*") }

        binding.publishBtn.setOnClickListener {
            val rating = binding.ratingBar.rating.toInt()
            val review = binding.reviewInput.text.toString().trim()
            if (rating <= 0 || review.isEmpty()) {
                Snackbar.make(binding.root, "Rating and review required", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val book = Book(args.bookId, args.bookTitle, args.bookAuthor, args.bookThumbnail)
            viewModel.createPost(book, rating, review, pickedImage)
        }
    }

    private fun observeViewModel() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.loading.isVisible = isLoading
            binding.publishBtn.isEnabled = !isLoading
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Snackbar.make(binding.root, "Posted successfully!", Snackbar.LENGTH_SHORT).show()
                viewModel.resetSaveSuccess()
                findNavController().popBackStack()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
