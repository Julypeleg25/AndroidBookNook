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
import com.colman.booknook.model.Model
import kotlinx.coroutines.launch

class EditPostFragment : Fragment(R.layout.fragment_create_post) {

    private lateinit var binding: FragmentCreatePostBinding
    private var pickedImage: Uri? = null
    private lateinit var postId: String

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pickedImage = uri
        if (uri != null) Toast.makeText(requireContext(), "Image selected", Toast.LENGTH_SHORT).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentCreatePostBinding.bind(view)
        postId = requireArguments().getString("postId").orEmpty()
        if (postId.isBlank()) {
            Toast.makeText(requireContext(), "Missing post id", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        // Reuse the create layout, but change button labels
        binding.publishBtn.text = "Save"
        binding.pickImageBtn.text = "Replace Image"

        Model.observePost(postId).observe(viewLifecycleOwner) { post ->
            if (post == null) return@observe
            binding.bookTitle.text = post.bookTitle
            binding.bookAuthor.text = post.bookAuthor
            binding.ratingBar.rating = post.rating.toFloat()
            binding.reviewInput.setText(post.review)
        }

        binding.pickImageBtn.setOnClickListener { pickImage.launch("image/*") }

        binding.publishBtn.setOnClickListener {
            val rating = binding.ratingBar.rating.toInt()
            val review = binding.reviewInput.text.toString().trim()
            if (rating <= 0 || review.isEmpty()) {
                Toast.makeText(requireContext(), "Rating and review required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            setLoading(true)
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    Model.updatePost(postId, rating, review, pickedImage)
                    Toast.makeText(requireContext(), "Updated", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), e.message ?: "Update failed", Toast.LENGTH_SHORT).show()
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
