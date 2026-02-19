package com.colman.booknook.ui.posts

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.colman.booknook.R
import com.colman.booknook.databinding.FragmentPostDetailsBinding
import com.colman.booknook.domain.Book
import com.colman.booknook.model.Model
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class PostDetailsFragment : Fragment(R.layout.fragment_post_details) {

    private lateinit var binding: FragmentPostDetailsBinding
    private lateinit var postId: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentPostDetailsBinding.bind(view)
        postId = PostDetailsFragmentArgs.fromBundle(requireArguments()).postId

        Model.observePost(postId).observe(viewLifecycleOwner) { post ->
            if (post == null) return@observe

            binding.title.text = post.bookTitle
            binding.author.text = post.bookAuthor
            binding.rating.rating = post.rating.toFloat()
            binding.review.text = post.review
            binding.meta.text = "${post.likesCount} likes • ${post.commentsCount} comments"

            if (!post.imageUrl.isNullOrBlank()) {
                Picasso.get().load(post.imageUrl).fit().centerCrop().into(binding.postImage)
            }
        }

        binding.likeBtn.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    Model.toggleLike(postId)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), e.message ?: "Like failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.addCommentBtn.setOnClickListener {
            val text = binding.commentInput.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            binding.commentInput.setText("")
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    Model.addComment(postId, text)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), e.message ?: "Comment failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.addWishlistBtn.setOnClickListener {
            val uid = Model.currentUserId() ?: return@setOnClickListener
            val post = Model.observePost(postId).value ?: return@setOnClickListener
            Model.addToWishlist(uid, Book(post.bookId, post.bookTitle, post.bookAuthor, post.bookThumbnail))
            Toast.makeText(requireContext(), "Added to wishlist", Toast.LENGTH_SHORT).show()
        }

        binding.title.setOnLongClickListener {
            val action = PostDetailsFragmentDirections.actionDetailsToEdit(postId)
            findNavController().navigate(action)
            true
        }
    }
}
