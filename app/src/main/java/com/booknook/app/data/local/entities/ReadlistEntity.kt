package com.booknook.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "readlist")
data class ReadlistEntity(
    @PrimaryKey override val key: String,
    override val userId: String,
    override val bookId: String,
    override val title: String,
    override val author: String,
    override val thumbnail: String?,
    override val addedAt: Long,
    override val genre: String? = null,
    override val publishedDate: String? = null,
    override val pageCount: Int? = null,
    override val description: String? = null
) : SavedBookListItem
