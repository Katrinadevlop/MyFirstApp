package ru.netology.nmedia.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import ru.netology.nmedia.R

class ConfirmSignOutDialog : DialogFragment() {

    interface ConfirmSignOutListener {
        fun onSignOutConfirmed()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.sign_out_confirm_title)
            .setMessage(R.string.sign_out_confirm_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                (parentFragment as? ConfirmSignOutListener)?.onSignOutConfirmed()
                    ?: (activity as? ConfirmSignOutListener)?.onSignOutConfirmed()
            }
            .setNegativeButton(R.string.no, null)
            .create()
    }

    companion object {
        const val TAG = "ConfirmSignOutDialog"
    }
}
