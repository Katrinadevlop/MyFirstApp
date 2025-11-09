package ru.netology.nmedia.activity

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.netology.nmedia.databinding.ActivityNewPostBinding

class NewPostActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNewPostBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val id = intent.getLongExtra(EXTRA_ID, 0L)
        val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
        binding.input.setText(text)
        if (text.isNotEmpty()) {
            binding.input.setSelection(text.length)
        }

        binding.btnSave.setOnClickListener {
            val content = binding.input.text?.toString().orEmpty().trim()
            if (content.isNotBlank()) {
                setResult(Activity.RESULT_OK, intent.putExtra(RESULT_KEY_TEXT, content).putExtra(RESULT_KEY_ID, id))
            }
            finish()
        }

        binding.btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    companion object {
        const val EXTRA_ID = "extra_post_id"
        const val EXTRA_TEXT = "extra_post_text"

        const val RESULT_KEY_ID = "result_post_id"
        const val RESULT_KEY_TEXT = "result_post_text"
    }
}
