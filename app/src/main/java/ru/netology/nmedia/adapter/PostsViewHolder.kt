package ru.netology.nmedia.adapter

import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardBinding
import ru.netology.nmedia.dto.Post
import kotlin.math.abs

class PostsViewHolder(
    private val binding: CardBinding,
    private val likeClickListener: LikeClickListener,
    private val shareClickListener: ShareClickListener,
    private val viewClickListener: ViewClickListener,
    private val removeClickListener: RemoveClickListener,
    private val editClickListener: EditClickListener,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(post: Post) = with(binding) {
        titleText.text = post.author
        dateText.text = post.published
        postText.text = post.content
        like.text = formatCount(post.likes)
        share.text = formatCount(post.shares)
        viewing.text = formatCount(post.views)
        like.isChecked = post.likedByMe

        // Video block visibility and click handling
        if (post.video.isNullOrBlank()) {
            videoContainer.visibility = View.GONE
        } else {
            videoContainer.visibility = View.VISIBLE
            val openVideo: (View) -> Unit = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.video))
                itemView.context.startActivity(intent)
            }
            videoPreview.setOnClickListener(openVideo)
            play.setOnClickListener(openVideo)
        }

        like.setOnClickListener { likeClickListener(post) }
        share.setOnClickListener { shareClickListener(post) }
        viewing.setOnClickListener { viewClickListener(post) }
        menuButton.setOnClickListener { view ->
            PopupMenu(view.context, view).apply {
                inflate(R.menu.menu)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.delete -> {
                            removeClickListener(post)
                            true
                        }
                        R.id.edit -> {
                            editClickListener(post)
                            true
                        }
                        else -> false
                    }
                }
            }.show()
        }
    }

    private fun formatCount(n: Int): String = shortCount(n.toLong())

    private fun shortCount(n: Long, useLatinUnits: Boolean = false): String {
        val k = if (useLatinUnits) "K" else "К"
        val m = if (useLatinUnits) "M" else "М"

        val sign = if (n < 0) "-" else ""
        val v = abs(n)

        return when {
            v < 1_000L -> sign + v.toString()

            v < 10_000L -> {
                val thousands = v / 1_000
                val hundreds = (v % 1_000) / 100
                "$sign$thousands.$hundreds$k"
            }

            v < 1_000_000L -> {
                val thousands = v / 1_000
                "$sign${thousands}$k"
            }

            else -> {
                val millions = v / 1_000_000
                val hundredThousands = (v % 1_000_000) / 100_000
                "$sign$millions.$hundredThousands$m"
            }
        }
    }
}
