package com.booknook.app.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.model.Model
import kotlinx.coroutines.launch

class FiltersViewModel : ViewModel() {

    private val query = MutableLiveData<FilterQuery>()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                Model.refreshPosts()
            } catch (_: Exception) { }
        }
    }

    val filteredPosts: LiveData<List<PostEntity>> = query.switchMap { q ->
        Model.searchPosts(q.title, q.author, q.minRating, q.minComments)
    }

    fun applyFilters(title: String?, author: String?, minRating: Int?, minComments: Int?) {
        query.value = FilterQuery(title, author, minRating, minComments)
    }

    data class FilterQuery(
        val title: String?,
        val author: String?,
        val minRating: Int?,
        val minComments: Int?
    )
}
