package ru.netology.nmedia.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dialog.SignInRequiredDialog
import ru.netology.nmedia.viewmodel.PostViewModel

class FeedFragment : Fragment(), SignInRequiredDialog.SignInRequiredListener {
    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = PostsAdapter(
            likeClickListener = { post ->
                if (AppAuth.isAuthenticated()) {
                    viewModel.like(post.id)
                } else {
                    SignInRequiredDialog().show(childFragmentManager, SignInRequiredDialog.TAG)
                }
            },
            shareClickListener = { viewModel.share(it.id) },
            viewClickListener = { viewModel.view(it.id) },
            removeClickListener = { viewModel.remove(it.id) },
            editClickListener = { post ->
                parentFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.container, EditPostFragment.newInstance(post.id, post.content))
                    .addToBackStack(null)
                    .commit()
            },
            postClickListener = { post ->
                parentFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.container, PostFragment.newInstance(post.id))
                    .addToBackStack(null)
                    .commit()
            },
            attachmentClickListener = { imageUrl ->
                parentFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.container, PhotoFragment.newInstance(imageUrl))
                    .addToBackStack(null)
                    .commit()
            }
        )

        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.data.collectLatest { posts ->
                adapter.submitList(posts)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.feedState.collectLatest { state ->
                binding.progress.isVisible = state.loading
                binding.errorGroup.isVisible = state.error
                binding.swipeRefresh.isRefreshing = false
            }
        }
        
        // Задание №1: обработка новых постов
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.newerPostsCount.collectLatest { count ->
                binding.newPostsBanner.isVisible = count > 0
            }
        }
        
        binding.newPostsBanner.setOnClickListener {
            viewModel.showNewerPosts()
            binding.list.smoothScrollToPosition(0)
        }
        
        // Задание №2: обработка несинхронизированных постов
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.unsyncedPostsCount.collectLatest { count ->
                binding.unsyncedPostsBanner.isVisible = count > 0
            }
        }
        
        binding.unsyncedPostsBanner.setOnClickListener {
            viewModel.retrySyncUnsavedPosts()
        }

        binding.retryButton.setOnClickListener {
            viewModel.loadPosts()
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        binding.fabAdd.setOnClickListener {
            if (AppAuth.isAuthenticated()) {
                parentFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.container, EditPostFragment.newInstance(0L, ""))
                    .addToBackStack(null)
                    .commit()
            } else {
                SignInRequiredDialog().show(childFragmentManager, SignInRequiredDialog.TAG)
            }
        }
    }

    override fun onSignInRequested() {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.container, SignInFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
