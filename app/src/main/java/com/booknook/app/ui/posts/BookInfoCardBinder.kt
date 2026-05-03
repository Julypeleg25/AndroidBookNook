package com.booknook.app.ui.posts

import android.content.Context
import android.text.TextUtils
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import com.booknook.app.R
import com.booknook.app.databinding.ViewBookInfoPanelBinding
import com.booknook.app.util.loadRemoteImage
import com.booknook.app.util.toShortGenreList

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
    private const val META_SEPARATOR = "  |  "

    fun bind(binding: ViewBookInfoPanelBinding, model: BookInfoCardModel) {
        val textColors = BookInfoTextColors.from(binding.root.context)
        bindHeader(binding, model)
        bindGenre(binding, model.genre)
        bindMeta(binding, model, textColors)
        bindDescription(binding, model.description, textColors)
    }

    private fun bindHeader(binding: ViewBookInfoPanelBinding, model: BookInfoCardModel) {
        binding.bookTitle.text = model.title
        binding.bookAuthor.text = model.author
        binding.bookThumb.loadRemoteImage(model.thumbnail, R.drawable.book_placeholder)
    }

    private fun bindGenre(binding: ViewBookInfoPanelBinding, rawGenre: String?) {
        val genre = rawGenre.toShortGenreList().orEmpty()
        binding.bookGenre.isVisible = genre.isNotEmpty()
        if (genre.isNotEmpty()) {
            binding.bookGenre.text = genre
        }
    }

    private fun bindMeta(
        binding: ViewBookInfoPanelBinding,
        model: BookInfoCardModel,
        textColors: BookInfoTextColors
    ) {
        val context = binding.root.context
        val metaParts = model.buildMetaParts(context)
        val hasMeta = metaParts.isNotEmpty()
        binding.bookMeta.text = if (hasMeta) {
            metaParts.joinToString(META_SEPARATOR)
        } else {
            context.getString(R.string.book_meta_missing)
        }
        binding.bookMeta.setTextColor(textColors.forContent(hasMeta))
    }

    private fun bindDescription(
        binding: ViewBookInfoPanelBinding,
        rawDescription: String?,
        textColors: BookInfoTextColors
    ) {
        val context = binding.root.context
        val description = rawDescription.toPlainDescription()
        val hasDescription = description.isNotEmpty()

        binding.bookDescription.text = if (hasDescription) {
            description
        } else {
            context.getString(R.string.book_description_missing)
        }
        binding.bookDescription.setTextColor(textColors.forContent(hasDescription))
        bindDescriptionToggle(binding, description, hasDescription)
    }

    private fun bindDescriptionToggle(
        binding: ViewBookInfoPanelBinding,
        description: String,
        hasDescription: Boolean
    ) {
        var isExpanded = binding.bookDescriptionToggle.tag as? Boolean ?: false
        if (binding.bookDescription.tag as? String != description) {
            isExpanded = false
        }

        binding.bookDescription.tag = description
        binding.bookDescription.maxLines = COLLAPSED_DESCRIPTION_LINES
        binding.bookDescriptionToggle.isVisible = false
        binding.bookDescriptionToggle.setOnClickListener {
            isExpanded = !isExpanded
            updateDescriptionState(binding, isExpanded)
        }

        binding.bookDescription.post {
            val shouldShowToggle = hasDescription && binding.bookDescription.isCollapsible()
            binding.bookDescriptionToggle.isVisible = shouldShowToggle
            if (!shouldShowToggle) {
                isExpanded = false
            }
            updateDescriptionState(binding, isExpanded)
        }
    }

    private fun updateDescriptionState(
        binding: ViewBookInfoPanelBinding,
        isExpanded: Boolean
    ) {
        binding.bookDescription.maxLines = if (isExpanded) {
            Int.MAX_VALUE
        } else {
            COLLAPSED_DESCRIPTION_LINES
        }
        binding.bookDescription.ellipsize = if (isExpanded) null else TextUtils.TruncateAt.END
        binding.bookDescriptionToggle.text = binding.root.context.getString(
            if (isExpanded) R.string.book_description_collapse else R.string.book_description_expand
        )
        binding.bookDescriptionToggle.tag = isExpanded
    }

    private fun BookInfoCardModel.buildMetaParts(context: Context): List<String> {
        return buildList {
            if (!publishedDate.isNullOrBlank()) {
                add(context.getString(R.string.book_meta_published_format, publishedDate))
            }
            if (pageCount != null && pageCount > 0) {
                add(context.resources.getQuantityString(R.plurals.book_pages, pageCount, pageCount))
            }
        }
    }

    private fun String?.toPlainDescription(): String {
        return HtmlCompat.fromHtml(
            this?.trim().orEmpty(),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        ).toString().trim()
    }

    private fun TextView.isCollapsible(): Boolean {
        return lineCount > COLLAPSED_DESCRIPTION_LINES || hasEllipsizedLine()
    }

    private fun TextView.hasEllipsizedLine(): Boolean {
        val currentLayout = layout ?: return false
        return (0 until currentLayout.lineCount).any { lineIndex ->
            currentLayout.getEllipsisCount(lineIndex) > 0
        }
    }

    private data class BookInfoTextColors(
        val primary: Int,
        val secondary: Int
    ) {
        fun forContent(hasContent: Boolean): Int = if (hasContent) primary else secondary

        companion object {
            fun from(context: Context): BookInfoTextColors {
                return BookInfoTextColors(
                    primary = ContextCompat.getColor(context, R.color.bn_brown_text),
                    secondary = ContextCompat.getColor(context, R.color.bn_secondary_text)
                )
            }
        }
    }
}
