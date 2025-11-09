package ru.netology.nmedia.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.databinding.ActivityMainBinding
import ru.netology.nmedia.viewmodel.PostViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: PostViewModel by viewModels()

    private val editorLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val content = data?.getStringExtra(NewPostActivity.RESULT_KEY_TEXT)?.trim().orEmpty()
            val id = data?.getLongExtra(NewPostActivity.RESULT_KEY_ID, 0L) ?: 0L
            if (content.isNotBlank()) {
                if (id == 0L) viewModel.create(content) else viewModel.update(id, content)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = PostsAdapter(
            likeClickListener = { viewModel.like(it.id) },
            shareClickListener = { viewModel.share(it.id) },
            viewClickListener = { viewModel.view(it.id) },
            removeClickListener = { viewModel.remove(it.id) },
            editClickListener = { post ->
                val intent = Intent(this, NewPostActivity::class.java)
                    .putExtra(NewPostActivity.EXTRA_ID, post.id)
                    .putExtra(NewPostActivity.EXTRA_TEXT, post.content)
                editorLauncher.launch(intent)
            }
        )

        binding.container.adapter = adapter

        viewModel.data.observe(this) { posts ->
            adapter.submitList(posts)
        }

        binding.fabAdd.setOnClickListener {
            val intent = Intent(this, NewPostActivity::class.java)
            editorLauncher.launch(intent)
        }
    }
}
