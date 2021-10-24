package com.example.cameraxapp

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

fun AppCompatActivity.showToast(message : String){
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun alertDialog(context: Context, cancellable : Boolean = true, dialogBuilder: AlertDialog.Builder.() -> Unit): Dialog {
    val builder = AlertDialog.Builder(context).also {
        it.setCancelable(cancellable)
        it.dialogBuilder()
    }
    return builder.create()
}

fun AlertDialog.Builder.negativeButton(text: String = "Dismiss", handleClick: (dialogInterface: DialogInterface) -> Unit = {}) {
    this.setPositiveButton(text) { dialogInterface, which -> handleClick(dialogInterface) }
}

fun AlertDialog.Builder.positiveButton(text: String = "Continue", handleClick: (dialogInterface: DialogInterface) -> Unit = {}) {
    this.setNegativeButton(text) { dialogInterface, which -> handleClick(dialogInterface) }
}