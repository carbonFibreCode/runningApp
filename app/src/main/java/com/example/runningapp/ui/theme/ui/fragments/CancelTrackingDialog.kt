package com.example.runningapp.ui.theme.ui.fragments

import android.app.Dialog
import androidx.fragment.app.DialogFragment
import android.os.Bundle
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.runningapp.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CancelTrackingDialog : DialogFragment() {

    private var yesListener: (() -> Unit)? = null

    fun setYesListener(listener : () -> Unit){
        yesListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("Cancel the Run")
            .setMessage("Are you sure to cancel the Current Run and Delete it's Current Data")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("Yes") {_,_ ->
                yesListener?.let {
                        yes -> yes()
                }
            }
            .setNegativeButton("No") {dialogInterface,_ ->
                dialogInterface.cancel()
            }
            .create()
    }
}