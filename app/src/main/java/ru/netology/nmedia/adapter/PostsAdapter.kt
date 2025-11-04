package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import ru.netology.nmedia.databinding.CardBinding
import ru.netology.nmedia.dto.Post

typealias LikeClickListener = (post: Post) -> Unit
typealias ShareClickListener = (post: Post) -> Unit
typealias ViewClickListener = (post: Post) -> Unit

class PostsAdapter(
    private val likeClickListener: LikeClickListener,
    private val shareClickListener: ShareClickListener,
    private val viewClickListener: ViewClickListener,
) : ListAdapter<Post, PostsViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): PostsViewHolder {
        val binding = CardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostsViewHolder(binding, likeClickListener, shareClickListener, viewClickListener)
    }

    override fun onBindViewHolder(
        holder: PostsViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position))
    }
}

class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem == newItem
}
