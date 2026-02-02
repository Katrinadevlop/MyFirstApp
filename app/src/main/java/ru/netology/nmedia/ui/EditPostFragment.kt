package ru.netology.nmedia.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.github.dhaval2404.imagepicker.ImagePicker
import com.bumptech.glide.Glide
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.databinding.FragmentEditPostBinding
import ru.netology.nmedia.dialog.ConfirmSignOutDialog
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.viewmodel.DraftViewModel
import ru.netology.nmedia.viewmodel.PostViewModel
import java.io.File

class EditPostFragment : Fragment(), ConfirmSignOutDialog.ConfirmSignOutListener {
    private var _binding: FragmentEditPostBinding? = null
    private val binding get() = _binding!!
    private val postViewModel: PostViewModel by activityViewModels()
    private val draftViewModel: DraftViewModel by viewModels()

    private var postId: Long = 0L
    private var initialText: String = ""
    
    private val pickPhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let {
                val file = File(requireContext().cacheDir, "photo_${System.currentTimeMillis()}.jpg")
                requireContext().contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                postViewModel.setPhoto(PhotoModel(uri = it, file = file))
            }
        }
    }

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
        
        // Добавляем меню с подтверждением выхода при редактировании
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
                val isAuthenticated = AppAuth.isAuthenticated()
                menu.findItem(R.id.sign_in)?.isVisible = !isAuthenticated
                menu.findItem(R.id.sign_up)?.isVisible = !isAuthenticated
                menu.findItem(R.id.sign_out)?.isVisible = isAuthenticated
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.sign_out -> {
                        // Показываем диалог подтверждения выхода
                        ConfirmSignOutDialog().show(childFragmentManager, ConfirmSignOutDialog.TAG)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
        
        // Наблюдаем за фото
        viewLifecycleOwner.lifecycleScope.launch {
            postViewModel.photo.collectLatest { photo ->
                binding.photoContainer.isVisible = photo != null
                photo?.uri?.let { uri ->
                    Glide.with(requireContext())
                        .load(uri)
                        .into(binding.photoPreview)
                }
            }
        }
        
        binding.btnAddPhoto.setOnClickListener {
            ImagePicker.with(this)
                .crop()
                .compress(2048)
                .createIntent { intent ->
                    pickPhotoLauncher.launch(intent)
                }
        }
        
        binding.btnRemovePhoto.setOnClickListener {
            postViewModel.clearPhoto()
        }

        if (postId == 0L) {
            viewLifecycleOwner.lifecycleScope.launch {
                draftViewModel.draft.collectLatest { content ->
                    val current = binding.input.text?.toString().orEmpty()
                    if (current != content) {
                        binding.input.setText(content)
                        binding.input.setSelection(binding.input.text?.length ?: 0)
                    }
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
            postViewModel.clearPhoto()
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

    override fun onSignOutConfirmed() {
        AppAuth.removeAuth()
        postViewModel.clearPhoto()
        parentFragmentManager.popBackStack()
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
