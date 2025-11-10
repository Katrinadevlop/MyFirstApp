package ru.netology.nmedia.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import ru.netology.nmedia.databinding.FragmentEditPostBinding
import ru.netology.nmedia.viewmodel.PostViewModel

class EditPostFragment : Fragment() {
    private var _binding: FragmentEditPostBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PostViewModel by activityViewModels()

    private var postId: Long = 0L
    private var initialText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            postId = it.getLong(ARG_ID)
            initialText = it.getString(ARG_TEXT).orEmpty()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.input.setText(initialText)
        if (initialText.isNotEmpty()) {
            binding.input.setSelection(initialText.length)
        }
        binding.btnSave.setOnClickListener {
            val content = binding.input.text?.toString().orEmpty().trim()
            if (content.isNotBlank()) {
                if (postId == 0L) viewModel.create(content) else viewModel.update(postId, content)
            }
            parentFragmentManager.popBackStack()
        }
        binding.btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ID = "post_id"
        private const val ARG_TEXT = "post_text"
        fun newInstance(id: Long, text: String) = EditPostFragment().apply {
            arguments = bundleOf(ARG_ID to id, ARG_TEXT to text)
        }
    }
}
