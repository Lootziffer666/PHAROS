package com.flow.pharos.desktop

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Pharos \u2013 File Sync"
    ) {
        MaterialTheme {
            SyncScreen()
        }
    }
}
