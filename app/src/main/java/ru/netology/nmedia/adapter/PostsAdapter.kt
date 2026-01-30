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
typealias RemoveClickListener = (post: Post) -> Unit
typealias EditClickListener = (post: Post) -> Unit
typealias PostClickListener = (post: Post) -> Unit
typealias AttachmentClickListener = (imageUrl: String) -> Unit

class PostsAdapter(
    private val likeClickListener: LikeClickListener,
    private val shareClickListener: ShareClickListener,
    private val viewClickListener: ViewClickListener,
    private val removeClickListener: RemoveClickListener,
    private val editClickListener: EditClickListener,
    private val postClickListener: PostClickListener,
    private val attachmentClickListener: AttachmentClickListener,
) : ListAdapter<Post, PostsViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): PostsViewHolder {
        val binding = CardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostsViewHolder(
            binding = binding,
            likeClickListener = likeClickListener,
            shareClickListener = shareClickListener,
            viewClickListener = viewClickListener,
            removeClickListener = removeClickListener,
            editClickListener = editClickListener,
            postClickListener = postClickListener,
            attachmentClickListener = attachmentClickListener,
        )
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
