package ru.netology.nmedia.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentPhotoBinding

class PhotoFragment : Fragment() {
    private var _binding: FragmentPhotoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Настройка AppBar с черным фоном
        (activity as? AppCompatActivity)?.supportActionBar?.let { actionBar ->
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(
                    android.graphics.Color.BLACK
                )
            )
        }

        val imageUrl = arguments?.getString(ARG_IMAGE_URL) ?: return

        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.netology)
            .error(R.drawable.netology)
            .timeout(60000)
            .into(binding.photoImage)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_IMAGE_URL = "image_url"

        fun newInstance(imageUrl: String) = PhotoFragment().apply {
            arguments = bundleOf(ARG_IMAGE_URL to imageUrl)
        }
    }
}
