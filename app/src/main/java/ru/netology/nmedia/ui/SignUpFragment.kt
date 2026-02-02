package ru.netology.nmedia.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.github.dhaval2404.imagepicker.ImagePicker
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nmedia.databinding.FragmentSignUpBinding
import ru.netology.nmedia.viewmodel.SignUpViewModel

class SignUpFragment : Fragment() {
    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignUpViewModel by viewModels()

    private val pickPhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when (result.resultCode) {
            ImagePicker.RESULT_ERROR -> {
                Toast.makeText(requireContext(), ImagePicker.getError(result.data), Toast.LENGTH_SHORT).show()
            }
            else -> {
                val uri = result.data?.data ?: return@registerForActivityResult
                viewModel.setAvatar(uri, uri.toFile())
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.signUpButton.setOnClickListener {
            val name = binding.name.text?.toString().orEmpty()
            val login = binding.login.text?.toString().orEmpty()
            val password = binding.password.text?.toString().orEmpty()
            val confirmPassword = binding.confirmPassword.text?.toString().orEmpty()
            viewModel.signUp(name, login, password, confirmPassword)
        }

        binding.btnAddAvatar.setOnClickListener {
            ImagePicker.with(this)
                .cropSquare()
                .compress(2048)
                .createIntent(pickPhotoLauncher::launch)
        }

        binding.btnRemoveAvatar.setOnClickListener {
            viewModel.clearAvatar()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.avatar.collectLatest { avatar ->
                if (avatar != null) {
                    binding.avatarPreview.setImageURI(avatar.uri)
                    binding.btnRemoveAvatar.isVisible = true
                } else {
                    binding.avatarPreview.setImageResource(android.R.drawable.ic_menu_camera)
                    binding.btnRemoveAvatar.isVisible = false
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                binding.progress.isVisible = state.loading
                binding.signUpButton.isEnabled = !state.loading

                if (state.error && state.errorMessage != null) {
                    Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_LONG).show()
                    viewModel.resetState()
                }

                if (state.success) {
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
