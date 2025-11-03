package ru.netology.nmedia

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.netology.nmedia.databinding.ActivityMainBinding
import java.util.Locale
import kotlin.math.floor

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var liked = false
        var likeCount = 0
        var shareCount = 0
        var viewCount = 0

        binding.textView.text = formatCount(likeCount)
        binding.shareId.text = formatCount(shareCount)

        binding.heart.setOnClickListener {
            liked = !liked
            likeCount = (likeCount + if (liked) 1 else -1).coerceAtLeast(0)
            binding.heart.setImageResource(
                if (liked) R.drawable.heart_red else R.drawable.heart
            )
            binding.textView.text = formatCount(likeCount)
        }

        binding.share.setOnClickListener {
            shareCount += 1
            binding.shareId.text = formatCount(shareCount)
        }

        binding.eyse.setOnClickListener {
            viewCount += 1
            binding.textView2.text = formatCount(viewCount)
        }
    }

    private fun formatCount(n: Int): String = shortCount(n.toLong())

    private fun shortCount(n: Long, useLatinUnits: Boolean = false): String {
        val k = if (useLatinUnits) "K" else "К"
        val m = if (useLatinUnits) "M" else "М"

        val sign = if (n < 0) "-" else ""
        val v = kotlin.math.abs(n)

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
