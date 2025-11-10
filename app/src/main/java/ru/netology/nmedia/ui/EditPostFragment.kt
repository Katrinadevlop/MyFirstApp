package ru.netology.nmedia.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import ru.netology.nmedia.databinding.FragmentEditPostBinding
import ru.netology.nmedia.viewmodel.DraftViewModel
import ru.netology.nmedia.viewmodel.PostViewModel

class EditPostFragment : Fragment() {
    private var _binding: FragmentEditPostBinding? = null
    private val binding get() = _binding!!
    private val postViewModel: PostViewModel by activityViewModels()
    private val draftViewModel: DraftViewModel by viewModels()

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

        if (postId == 0L) {
            draftViewModel.draft.observe(viewLifecycleOwner) { content ->
                val current = binding.input.text?.toString().orEmpty()
                if (current != content) {
                    binding.input.setText(content)
                    binding.input.setSelection(binding.input.text?.length ?: 0)
                }
            }
            binding.input.doOnTextChanged { text, _, _, _ ->
                draftViewModel.onContentChanged(text?.toString().orEmpty())
            }
        } else {
            binding.input.setText(initialText)
            if (initialText.isNotEmpty()) {
                binding.input.setSelection(initialText.length)
            }
        }

        binding.btnSave.setOnClickListener {
            val content = binding.input.text?.toString().orEmpty().trim()
            if (content.isNotBlank()) {
                if (postId == 0L) {
                    postViewModel.create(content)
                    draftViewModel.clear()
                } else {
                    postViewModel.update(postId, content)
                }
            }
            parentFragmentManager.popBackStack()
        }
        binding.btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (postId == 0L) {
                draftViewModel.saveNow(binding.input.text?.toString().orEmpty())
            }
            isEnabled = false
            requireActivity().onBackPressedDispatcher.onBackPressed()
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
