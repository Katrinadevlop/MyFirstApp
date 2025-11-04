package ru.netology.nmedia.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.ActivityMainBinding
import ru.netology.nmedia.viewmodel.PostViewModel
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: PostViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.data.observe(this) { post ->
            with(binding) {
                titleText.text = post.author
                dateText.text = post.published
                postText.text = post.content
                numberLike.text = formatCount(post.likes)
                numberShare.text = formatCount(post.shares)
                numberViews.text = formatCount(post.views)
                like.setImageResource(
                    if (post.likedByMe) R.drawable.ic_heart_red else R.drawable.ic_heart
                )
            }
        }

        binding.like.setOnClickListener { viewModel.like() }
        binding.share.setOnClickListener { viewModel.share() }
        binding.viewing.setOnClickListener { viewModel.view() }
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
