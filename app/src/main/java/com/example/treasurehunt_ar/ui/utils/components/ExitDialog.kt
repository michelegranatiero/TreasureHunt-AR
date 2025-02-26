package com.example.treasurehunt_ar.ui.utils.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ExitDialog(
    title: String = "Exit?",
    text: String = "Do you really want to exit?",
    confirmText: String = "Yes",
    dismissText: String = "No",
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
){
    AlertDialog(
        onDismissRequest = { onDismissRequest() },
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            Button(onClick = { onConfirm() }) {
                Text(confirmText)
            }
        },
        dismissButton = {
            Button(onClick = { onDismissRequest() }) {
                Text(dismissText)
            }
        }
    )
}