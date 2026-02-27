package com.colman.booknook.ui.posts

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.colman.booknook.R
import com.colman.booknook.databinding.FragmentCreatePostBinding
import com.colman.booknook.domain.Book
import com.colman.booknook.model.Model
import kotlinx.coroutines.launch

class CreatePostFragment : Fragment(R.layout.fragment_create_post) {

    private lateinit var binding: FragmentCreatePostBinding
    private var pickedImage: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pickedImage = uri
        if (uri != null) Toast.makeText(requireContext(), "Image selected", Toast.LENGTH_SHORT).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentCreatePostBinding.bind(view)

        val bookId = requireArguments().getString("bookId").orEmpty()
        val bookTitle = requireArguments().getString("bookTitle").orEmpty()
        val bookAuthor = requireArguments().getString("bookAuthor").orEmpty()
        val bookThumbnail = requireArguments().getString("bookThumbnail")
        if (bookId.isBlank() || bookTitle.isBlank() || bookAuthor.isBlank()) {
            Toast.makeText(requireContext(), "Missing book details", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        binding.bookTitle.text = bookTitle
        binding.bookAuthor.text = bookAuthor

        binding.pickImageBtn.setOnClickListener { pickImage.launch("image/*") }

        binding.publishBtn.setOnClickListener {
            val rating = binding.ratingBar.rating.toInt()
            val review = binding.reviewInput.text.toString().trim()
            if (rating <= 0 || review.isEmpty()) {
                Toast.makeText(requireContext(), "Rating and review required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val book = Book(bookId, bookTitle, bookAuthor, bookThumbnail)

            setLoading(true)
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    Model.createPost(book, rating, review, pickedImage)
                    Toast.makeText(requireContext(), "Posted!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), e.message ?: "Publish failed", Toast.LENGTH_SHORT).show()
                } finally {
                    setLoading(false)
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.publishBtn.isEnabled = !isLoading
    }
}
