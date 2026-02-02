package ru.netology.nmedia.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import ru.netology.nmedia.R

class SignInRequiredDialog : DialogFragment() {

    interface SignInRequiredListener {
        fun onSignInRequested()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.sign_in_required_title)
            .setMessage(R.string.sign_in_required_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                (parentFragment as? SignInRequiredListener)?.onSignInRequested()
                    ?: (activity as? SignInRequiredListener)?.onSignInRequested()
            }
            .setNegativeButton(R.string.no, null)
            .create()
    }

    companion object {
        const val TAG = "SignInRequiredDialog"
    }
}
