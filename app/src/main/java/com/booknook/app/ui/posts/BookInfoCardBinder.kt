package com.booknook.app.ui.posts

import android.text.TextUtils
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import com.booknook.app.R
import com.booknook.app.databinding.ViewBookInfoPanelBinding
import com.squareup.picasso.Picasso

data class BookInfoCardModel(
    val title: String,
    val author: String,
    val thumbnail: String?,
    val genre: String?,
    val publishedDate: String?,
    val pageCount: Int?,
    val description: String?
)

object BookInfoCardBinder {
    private const val COLLAPSED_DESCRIPTION_LINES = 4

    fun bind(binding: ViewBookInfoPanelBinding, model: BookInfoCardModel) {
        val context = binding.root.context
        val primaryTextColor = ContextCompat.getColor(context, R.color.bn_brown_text)
        val secondaryTextColor = ContextCompat.getColor(context, R.color.bn_secondary_text)

        binding.bookTitle.text = model.title
        binding.bookAuthor.text = model.author

        Picasso.get()
            .load(model.thumbnail)
            .placeholder(R.drawable.book_placeholder)
            .error(R.drawable.book_placeholder)
            .fit()
            .centerCrop()
            .into(binding.bookThumb)

        val genre = model.genre?.trim().orEmpty()
        binding.bookGenre.isVisible = genre.isNotEmpty()
        if (genre.isNotEmpty()) {
            binding.bookGenre.text = genre
        }

        val metaParts = buildList {
            if (!model.publishedDate.isNullOrBlank()) {
                add(context.getString(R.string.book_meta_published_format, model.publishedDate))
            }
            if (model.pageCount != null && model.pageCount > 0) {
                add(context.resources.getQuantityString(R.plurals.book_pages, model.pageCount, model.pageCount))
            }
        }

        val hasMeta = metaParts.isNotEmpty()
        binding.bookMeta.text = if (hasMeta) {
            metaParts.joinToString("  •  ")
        } else {
            context.getString(R.string.book_meta_missing)
        }
        binding.bookMeta.setTextColor(if (hasMeta) primaryTextColor else secondaryTextColor)

        val rawDescription = model.description?.trim().orEmpty()
        val description = HtmlCompat.fromHtml(
            rawDescription,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        ).toString().trim()
        val hasDescription = description.isNotEmpty()
        binding.bookDescription.text = if (hasDescription) description else context.getString(R.string.book_description_missing)
        binding.bookDescription.setTextColor(if (hasDescription) primaryTextColor else secondaryTextColor)

        val previousDescription = binding.bookDescription.tag as? String
        var isDescriptionExpanded = binding.bookDescriptionToggle.tag as? Boolean ?: false
        if (previousDescription != description) {
            isDescriptionExpanded = false
        }
        binding.bookDescription.tag = description

        fun updateDescriptionState() {
            binding.bookDescription.maxLines = if (isDescriptionExpanded) {
                Int.MAX_VALUE
            } else {
                COLLAPSED_DESCRIPTION_LINES
            }
            binding.bookDescription.ellipsize = if (isDescriptionExpanded) null else TextUtils.TruncateAt.END
            binding.bookDescriptionToggle.text = context.getString(
                if (isDescriptionExpanded) R.string.book_description_collapse else R.string.book_description_expand
            )
            binding.bookDescriptionToggle.tag = isDescriptionExpanded
        }

        binding.bookDescription.maxLines = COLLAPSED_DESCRIPTION_LINES
        binding.bookDescriptionToggle.isVisible = false
        binding.bookDescriptionToggle.setOnClickListener {
            isDescriptionExpanded = !isDescriptionExpanded
            updateDescriptionState()
        }

        binding.bookDescription.post {
            val layout = binding.bookDescription.layout
            val isEllipsized = layout != null && (0 until layout.lineCount).any { lineIndex ->
                layout.getEllipsisCount(lineIndex) > 0
            }
            val shouldShowToggle = hasDescription && (
                binding.bookDescription.lineCount > COLLAPSED_DESCRIPTION_LINES || isEllipsized
            )
            binding.bookDescriptionToggle.isVisible = shouldShowToggle
            if (!shouldShowToggle) {
                isDescriptionExpanded = false
            }
            updateDescriptionState()
        }
    }
}
