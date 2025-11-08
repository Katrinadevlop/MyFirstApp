package ru.netology.nmedia.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import android.view.inputmethod.EditorInfo
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.databinding.ActivityMainBinding
import ru.netology.nmedia.viewmodel.PostViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: PostViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = PostsAdapter(
            likeClickListener = { viewModel.like(it.id) },
            shareClickListener = { viewModel.share(it.id) },
            viewClickListener = { viewModel.view(it.id) },
            removeClickListener = { viewModel.remove(it.id) },
            addClickListener = {
                val text = binding.input.text?.toString().orEmpty().trim()
                if (text.isNotEmpty()) {
                    viewModel.add(text)
                    binding.input.setText("")

                }
            },
            editClickListener = { post ->
                binding.input.requestFocus()
                binding.input.setText(post.content)
                binding.input.setSelection(binding.input.text?.length ?: 0)
                viewModel.edit(post)
            }
        )

        binding.container.adapter = adapter

        viewModel.data.observe(this) { posts ->
            adapter.submitList(posts)
        }

        binding.input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                val text = binding.input.text?.toString().orEmpty().trim()
                if (text.isNotEmpty()) {
                    viewModel.add(text)
                    binding.input.setText("")
                }
                true
            } else false
        }
    }
}
