package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.databinding.ActivityMainBinding
import ru.netology.nmedia.ui.FeedFragment
import ru.netology.nmedia.ui.SignInFragment
import ru.netology.nmedia.ui.SignUpFragment
import ru.netology.nmedia.viewmodel.PostViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: PostViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(binding.container.id, FeedFragment())
                .commit()
        }

        // Обновляем меню при изменении состояния аутентификации
        lifecycleScope.launch {
            AppAuth.authState.collectLatest {
                invalidateOptionsMenu()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val isAuthenticated = AppAuth.isAuthenticated()
        menu?.findItem(R.id.sign_in)?.isVisible = !isAuthenticated
        menu?.findItem(R.id.sign_up)?.isVisible = !isAuthenticated
        menu?.findItem(R.id.sign_out)?.isVisible = isAuthenticated
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sign_in -> {
                supportFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(binding.container.id, SignInFragment())
                    .addToBackStack(null)
                    .commit()
                true
            }
            R.id.sign_up -> {
                supportFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(binding.container.id, SignUpFragment())
                    .addToBackStack(null)
                    .commit()
                true
            }
            R.id.sign_out -> {
                AppAuth.removeAuth()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
