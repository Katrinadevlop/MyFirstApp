package ru.netology.nmedia.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentPostBinding
import ru.netology.nmedia.viewmodel.PostViewModel

class PostFragment : Fragment() {
    private var _binding: FragmentPostBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PostViewModel by activityViewModels()

    private var postId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postId = requireArguments().getLong(ARG_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // observe and bind
        viewModel.data.observe(viewLifecycleOwner) { posts ->
            val post = posts.firstOrNull { it.id == postId } ?: return@observe
            with(binding.post) {
                titleText.text = post.author
                dateText.text = post.published
                postText.text = post.content

                like.isChecked = post.likedByMe
                like.text = post.likes.toString()
                share.text = post.shares.toString()
                viewing.text = post.views.toString()

                like.setOnClickListener { viewModel.like(post.id) }
                share.setOnClickListener { viewModel.share(post.id) }
                viewing.setOnClickListener { viewModel.view(post.id) }

                if (post.video.isNullOrBlank()) {
                    videoContainer.visibility = View.GONE
                } else {
                    videoContainer.visibility = View.VISIBLE
                    val openVideo: (View) -> Unit = {
                        // Use ACTION_VIEW to open in external app
                        startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(post.video)))
                    }
                    videoPreview.setOnClickListener(openVideo)
                    play.setOnClickListener(openVideo)
                }

                menuButton.setOnClickListener { v ->
                    PopupMenu(v.context, v).apply {
                        inflate(R.menu.menu)
                        setOnMenuItemClickListener { item ->
                            when (item.itemId) {
                                R.id.edit -> {
                                    parentFragmentManager.beginTransaction()
                                        .setReorderingAllowed(true)
                                        .replace(R.id.container, EditPostFragment.newInstance(post.id, post.content))
                                        .addToBackStack(null)
                                        .commit()
                                    true
                                }
                                R.id.delete -> {
                                    viewModel.remove(post.id)
                                    parentFragmentManager.popBackStack()
                                    true
                                }
                                else -> false
                            }
                        }
                    }.show()
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ID = "post_id"
        fun newInstance(id: Long) = PostFragment().apply {
            arguments = bundleOf(ARG_ID to id)
        }
    }
}
